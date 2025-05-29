package com.unpam.view;
import com.unpam.model.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.sql.*;
import javax.sql.DataSource;
import javax.naming.*;

@WebServlet(name = "MainForm", urlPatterns = {"/MainForm"})
public class MainForm extends HttpServlet {
    
    private Connection getConnection() throws SQLException {
        Koneksi koneksi = new Koneksi();
        Connection conn = koneksi.getConnection();
        if(conn == null) {
            throw new SQLException("Gagal koneksi: " + koneksi.getPesanKesalahan());
        }
        return conn;
    }

    private int getTotalRecords(String tableName) {
        int total = 0;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                total = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    // Method untuk validasi login dengan enkripsi MD5
    private boolean validateLogin(String ktp, String password, HttpServletRequest request) {
        try (Connection conn = getConnection()) {
            // Debug: cek koneksi
            if (conn == null) {
                System.out.println("DEBUG: Koneksi database gagal");
                return false;
            }
            
            // Enkripsi password input menggunakan MD5
            Enkripsi enkripsi = new Enkripsi();
            String hashedPassword = enkripsi.hashMD5(password);
            
            String sql = "SELECT nama, ktp FROM tbkaryawan WHERE ktp = ? AND password = ?";
            System.out.println("DEBUG: SQL Query: " + sql);
            System.out.println("DEBUG: Input KTP: " + ktp);
            System.out.println("DEBUG: Input Password (plain): " + password);
            System.out.println("DEBUG: Input Password (hashed): " + hashedPassword);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ktp);
                stmt.setString(2, hashedPassword); // Gunakan password yang sudah di-hash
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Login berhasil, simpan data ke session
                        HttpSession session = request.getSession();
                        session.setAttribute("isLoggedIn", true);
                        session.setAttribute("userKtp", rs.getString("ktp"));
                        session.setAttribute("userName", rs.getString("nama"));
                        
                        System.out.println("DEBUG: Login berhasil untuk user: " + rs.getString("nama"));
                        return true;
                    } else {
                        System.out.println("DEBUG: Tidak ada data yang cocok");
                        
                        // Debug: cek apakah data ada di database
                        String checkSql = "SELECT ktp, nama, password FROM tbkaryawan WHERE ktp = ?";
                        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                            checkStmt.setString(1, ktp);
                            try (ResultSet checkRs = checkStmt.executeQuery()) {
                                if (checkRs.next()) {
                                    System.out.println("DEBUG: Data ditemukan untuk KTP: " + ktp);
                                    System.out.println("DEBUG: Password di DB: " + checkRs.getString("password"));
                                    System.out.println("DEBUG: Password input (hashed): " + hashedPassword);
                                    System.out.println("DEBUG: Password match: " + checkRs.getString("password").equals(hashedPassword));
                                } else {
                                    System.out.println("DEBUG: KTP tidak ditemukan di database: " + ktp);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error saat validasi login: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Method untuk menambah user test (untuk debugging) dengan password terenkripsi
    private void createTestUser() {
        try (Connection conn = getConnection()) {
            Enkripsi enkripsi = new Enkripsi();
            String hashedPassword = enkripsi.hashMD5("123456"); // Password plain: 123456
            
            String sql = "INSERT INTO tbkaryawan (ktp, nama, ruang, password, kode_pekerjaan) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "1234567890123456");
                stmt.setString(2, "Test User");
                stmt.setInt(3, 1);
                stmt.setString(4, hashedPassword);
                stmt.setString(5, "P001");
                stmt.setString(6, hashedPassword);
                
                int result = stmt.executeUpdate();
                System.out.println("DEBUG: Test user created/updated, affected rows: " + result);
                System.out.println("DEBUG: Test user password (hashed): " + hashedPassword);
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error creating test user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method untuk logout
    private void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("isLoggedIn");
            session.removeAttribute("userKtp");
            session.removeAttribute("userName");
        }
    }

    // Method untuk generate login form
    private String generateLoginForm(String errorMessage) {
        String error = errorMessage != null ? 
            "<div style='background:#f8d7da;color:#721c24;padding:10px;border-radius:5px;margin-bottom:15px;'>" + 
            errorMessage + "</div>" : "";
            
        return "<div class='card' style='max-width:400px;margin:2rem auto;'>" +
               "  <h2 style='text-align:center;color:var(--primary);margin-bottom:2rem;'>Login Sistem</h2>" +
               error +
               "  <form method='post' action='MainForm'>" +
               "    <div style='margin-bottom:1rem;'>" +
               "      <label style='display:block;margin-bottom:5px;font-weight:bold;'>KTP:</label>" +
               "      <input type='text' name='ktp' required style='width:100%;padding:10px;border:1px solid #ddd;border-radius:5px;' placeholder='Masukkan nomor KTP'>" +
               "    </div>" +
               "    <div style='margin-bottom:1.5rem;'>" +
               "      <label style='display:block;margin-bottom:5px;font-weight:bold;'>Password:</label>" +
               "      <input type='password' name='password' required style='width:100%;padding:10px;border:1px solid #ddd;border-radius:5px;' placeholder='Masukkan password'>" +
               "    </div>" +
               "    <button type='submit' name='action' value='login' style='width:100%;padding:12px;background:var(--primary);color:white;border:none;border-radius:5px;font-size:1rem;cursor:pointer;'>" +
               "      <i class='fas fa-sign-in-alt'></i> Login" +
               "    </button>" +
               "  </form>" +
               "  <div style='margin-top:1rem;padding:10px;background:#e7f3ff;border-radius:5px;font-size:0.9em;'>" +
               "    <strong>Info:</strong> Gunakan KTP dan password yang sudah terdaftar di sistem." +
               "  </div>" +
               "</div>";
    }

    public void tampilkan(HttpServletRequest request, HttpServletResponse response, String konten)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        HttpSession session = request.getSession(true);

        // Cek status login
        boolean isLoggedIn = session.getAttribute("isLoggedIn") != null && 
                           (Boolean) session.getAttribute("isLoggedIn");
        String userName = (String) session.getAttribute("userName");
        String userKtp = (String) session.getAttribute("userKtp");

        // Get active menu from request attribute (set by controllers) or determine from path
        String activeMenu = (String) request.getAttribute("activeMenu");
        if (activeMenu == null) {
            String path = request.getServletPath();
            if (path.equals("/MainForm")) {
                activeMenu = "home";
            } else if (path.contains("KaryawanController") || path.contains("PekerjaanController")) {
                activeMenu = "master";
            } else if (path.contains("GajiController")) {
                activeMenu = "transaksi";
            } else if (path.contains("Laporan")) {
                activeMenu = "laporan";
            } else {
                activeMenu = "home";
            }
        }

        // Get data from database
        int totalKaryawan = getTotalRecords("tbkaryawan");
        int totalPekerjaan = getTotalRecords("tbpekerjaan");

        // Generate user menu berdasarkan status login
        String userMenu;
        if (isLoggedIn) {
            userMenu = 
                "<div class='dropdown'>" +
                "  <a href='#' class='nav-link user-menu'>" +
                "    <i class='fas fa-user-circle'></i> " +
                "  </a>" +
                "  <div class='dropdown-content'>" +
                "    <div style='padding:10px 16px;border-bottom:1px solid #eee;font-size:0.9em;color:#666;'>" +
                "      <strong>" + userName + "</strong><br>" +
                "      KTP: " + userKtp +
                "    </div>" +
                "    <a href='MainForm?action=logout'><i class='fas fa-sign-out-alt'></i> Logout</a>" +
                "  </div>" +
                "</div>";
        } else {
            userMenu = "<a href='MainForm?showLogin=true' class='login-btn'><i class='fas fa-sign-in-alt'></i> Login</a>";
        }

        String topMenu = 
            "<nav class='navbar'>" +
            "  <div class='nav-container'>" +
            "    <a href='MainForm' class='logo'>PT. RAFKY FERDIAN BERJAYA</a>" +
            "    <div class='nav-links'>" +
            "      <a href='MainForm' class='nav-link" + (activeMenu.equals("home") ? " active" : "") + "'>Home</a>" +
            "      <div class='dropdown'>" +
            "        <a href='#' class='nav-link" + (activeMenu.equals("master") ? " active" : "") + "'>Master Data</a>" +
            "        <div class='dropdown-content'>" +
            "          <a href='KaryawanController'>Karyawan</a>" +
            "          <a href='PekerjaanController'>Pekerjaan</a>" +
            "        </div>" +
            "      </div>" +
            "      <div class='dropdown'>" +
            "        <a href='#' class='nav-link" + (activeMenu.equals("transaksi") ? " active" : "") + "'>Transaksi</a>" +
            "        <div class='dropdown-content'>" +
            "          <a href='GajiController'>Penggajian</a>" +
            "        </div>" +
            "      </div>" +
            "      <div class='dropdown'>" +
            "        <a href='#' class='nav-link" + (activeMenu.equals("laporan") ? " active" : "") + "'>Laporan</a>" +
            "        <div class='dropdown-content'>" +
            "          <a href='LaporanKaryawan'>Data Karyawan</a>" +
            "          <a href='LaporanPekerjaan'>Data Pekerjaan</a>" +
            "          <hr style='margin:5px 0;border-color:#eee'>" +
            "          <a href='LaporanGaji'>Gaji Karyawan</a>" +
            "        </div>" +
            "      </div>" +
            userMenu +
            "    </div>" +
            "  </div>" +
            "</nav>";

        String defaultKonten =
            "<div class='hero' style='background:linear-gradient(135deg, var(--primary), var(--secondary));padding:4rem;border-radius:10px;color:white;margin-bottom:2rem;text-align:center;'>" +
            "  <h1 style='font-size:2.5em;margin-bottom:1rem;text-shadow:2px 2px 4px rgba(0,0,0,0.3);'>Selamat Datang di PT. Rafky Ferdian Berjaya</h1>" +
            "  <p style='font-size:1.2em;margin-bottom:2rem;'>Sistem Informasi Terintegrasi untuk Manajemen Karyawan, Pekerjaan, dan Penggajian</p>" +
            (isLoggedIn ? 
                "<div style='background:rgba(255,255,255,0.2);padding:1rem;border-radius:8px;display:inline-block;'>" +
                "  <i class='fas fa-user-check'></i> Hallo, <strong>" + userName + "</strong>!" +
                "</div>" : 
                "<p style='font-size:1em;opacity:0.9;'></p>"
            ) +
            "</div>" +
            
            "<div style='display:flex;gap:2rem;margin-bottom:2rem;flex-wrap:wrap;'>" +
            "  <div class='card' style='flex:1 1 250px;text-align:center;min-width:250px;'>" +
            "    <h2 style='color:var(--primary);margin-bottom:1rem;'><i class='fas fa-users'></i> Total Karyawan</h2>" +
            "    <p style='font-size:2.5em;color:var(--secondary);font-weight:bold;'>" + totalKaryawan + "</p>" +
            "  </div>" +
            "  <div class='card' style='flex:1 1 250px;text-align:center;min-width:250px;'>" +
            "    <h2 style='color:var(--primary);margin-bottom:1rem;'><i class='fas fa-briefcase'></i> Total Pekerjaan</h2>" +
            "    <p style='font-size:2.5em;color:var(--secondary);font-weight:bold;'>" + totalPekerjaan + "</p>" +
            "  </div>" +
            "</div>" +
            
            "<div class='card'>" +
            "  <h2 style='color:var(--primary);margin-bottom:2rem;text-align:center;'>Fitur Utama Sistem</h2>" +
            "  <div style='display:grid;grid-template-columns:repeat(auto-fit, minmax(250px, 1fr));gap:1.5rem;'>" +
            "    <div class='feature-card'>" +
            "      <h3><i class='fas fa-user-cog'></i> Manajemen Karyawan</h3>" +
            "      <p>Kelola data karyawan lengkap dengan informasi personal dan pekerjaan</p>" +
            "    </div>" +
            "    <div class='feature-card'>" +
            "      <h3><i class='fas fa-tasks'></i> Manajemen Pekerjaan</h3>" +
            "      <p>Pengelolaan jenis pekerjaan dan penugasan proyek</p>" +
            "    </div>" +
            "    <div class='feature-card'>" +
            "      <h3><i class='fas fa-calculator'></i> Sistem Penggajian</h3>" +
            "      <p>Perhitungan gaji otomatis dengan berbagai komponen tunjangan</p>" +
            "    </div>" +
            "    <div class='feature-card'>" +
            "      <h3><i class='fas fa-chart-pie'></i> Laporan Real-time</h3>" +
            "      <p>Laporan data real-time dalam bentuk format yang beragam</p>" +
            "    </div>" +
            "  </div>" +
            "</div>";

        String footer = 
            "<footer style='background:var(--primary);color:white;padding:1.5rem;margin-top:auto;'>" +
            "  <div style='max-width:1200px;margin:0 auto;text-align:center;'>" +
            "    <p style='margin-bottom:0.5rem;'>&copy; 2025 PT. Rafky Ferdian Berjaya</p>" +
            "    <div style='display:flex;justify-content:center;gap:1rem;font-size:0.9em;opacity:0.8;'>" +
            "      <span><i class='fas fa-phone'></i> (+62) 857-5932-8890</span>" +
            "      <span><i class='fas fa-envelope'></i> me@rafkyferdian.my.id</span>" +
            "      <span><i class='fas fa-map-marker-alt'></i> Tangerang Selatan, Indonesia</span>" +
            "    </div>" +
            "  </div>" +
            "</footer>";

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang='en' style='height:100%;'>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            out.println("<title>PT. RAFKY FERDIAN BERJAYA</title>");
            out.println("<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css'>");
            out.println("<style>");
            out.println(":root { --primary: #2c3e50; --secondary: #3498db; --light: #ecf0f1; }");
            out.println("* { box-sizing: border-box; margin: 0; font-family: 'Segoe UI', sans-serif; }");
            out.println(".navbar { background: var(--primary); padding: 1rem; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }");
            out.println(".nav-container { max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; }");
            out.println(".logo { color: white; font-size: 1.5rem; text-decoration: none; font-weight: bold; }");
            out.println(".nav-links { display: flex; gap: 1.5rem; align-items: center; }");
            out.println(".dropdown { position: relative; }");
            out.println(".nav-link { background: none; border: none; color: white; padding: 0.8rem; cursor: pointer; font-size: 1rem; text-decoration: none; transition: all 0.3s; }");
            out.println(".nav-link.active { border-bottom: 3px solid var(--secondary); padding-bottom: 5px; }");
            out.println(".user-menu { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }");
            out.println(".dropdown-content { display: none; position: absolute; background: white; min-width: 200px; box-shadow: 0 8px 16px rgba(0,0,0,0.1); z-index: 1; right: 0; }");
            out.println(".dropdown:hover .dropdown-content { display: block; }");
            out.println(".dropdown-content a { color: var(--primary); padding: 12px 16px; text-decoration: none; display: block; }");
            out.println(".dropdown-content a:hover { background: var(--light); }");
            out.println(".login-btn { background: var(--secondary); color: white; padding: 0.5rem 1rem; border-radius: 5px; text-decoration: none; }");
            out.println(".container { max-width: 1200px; margin: 2rem auto; padding: 0 1rem; }");
            out.println(".card { background: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); padding: 2rem; margin-bottom: 2rem; }");
            out.println(".feature-card { padding: 1.5rem; border-radius: 8px; background: #f8f9fa; transition: transform 0.3s; }");
            out.println(".feature-card:hover { transform: translateY(-5px); }");
            out.println(".feature-card h3 { color: var(--primary); margin-bottom: 1rem; font-size: 1.2em; }");
            out.println(".feature-card i { margin-right: 10px; color: var(--secondary); }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body style='display:flex; flex-direction:column; min-height:100vh;'>");
            out.println(topMenu);
            out.println("<div class='container' style='flex:1;'>");
            out.println(konten.isEmpty() ? defaultKonten : konten);
            out.println("</div>");
            out.println(footer);
            out.println("</body>");
            out.println("</html>");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        String showLogin = request.getParameter("showLogin");
        
        // Handle logout
        if ("logout".equals(action)) {
            logout(request);
            response.sendRedirect("MainForm");
            return;
        }
        
        // Show login form
        if ("true".equals(showLogin)) {
            String loginForm = generateLoginForm(null);
            tampilkan(request, response, loginForm);
            return;
        }
        
        String konten = request.getAttribute("konten") != null ? 
            (String) request.getAttribute("konten") : "";
        tampilkan(request, response, konten);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        if ("login".equals(action)) {
            String ktp = request.getParameter("ktp");
            String password = request.getParameter("password");
            
            // Uncomment baris berikut untuk membuat test user (hanya untuk testing)
            // createTestUser();
            
            if (validateLogin(ktp, password, request)) {
                // Login berhasil, redirect ke home
                response.sendRedirect("MainForm");
            } else {
                // Login gagal, tampilkan form login dengan pesan error
                String loginForm = generateLoginForm("KTP atau password salah!");
                tampilkan(request, response, loginForm);
            }
            return;
        }
        
        // Default behavior
        doGet(request, response);
    }
}