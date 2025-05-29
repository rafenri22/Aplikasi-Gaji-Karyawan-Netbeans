package com.unpam.view;

import com.unpam.model.Koneksi;
import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import net.sf.jasperreports.engine.*;

@WebServlet(name = "LaporanKaryawan", urlPatterns = {"/LaporanKaryawan"})
public class LaporanKaryawan extends HttpServlet {
    
    private Connection getConnection() throws SQLException {
        Koneksi koneksi = new Koneksi();
        Connection conn = koneksi.getConnection();
        if (conn == null) {
            throw new SQLException("Gagal koneksi: " + koneksi.getPesanKesalahan());
        }
        return conn;
    }

    private void generateReport(HttpServletRequest request, HttpServletResponse response, 
            String[] selectedKtp, String kodePekerjaan, Integer ruang, String filterType) {
        
        Connection conn = null;
        try {
            conn = getConnection();
            
            // Path ke file JRXML
            String reportPath = getServletContext().getRealPath("/reports/laporan_karyawan.jasper");
            
            // Parameter report
            HashMap<String, Object> params = new HashMap<>();
            
            // Handle different filter types
            switch(filterType) {
                case "all":
                    break;
                case "single":
                    if(selectedKtp != null && selectedKtp.length > 0) {
                        params.put("ktp_param", selectedKtp[0]);
                    }
                    break;
                case "multiple":
                    if(selectedKtp != null && selectedKtp.length > 0) {
                        String ktpList = String.join(",", selectedKtp);
                        params.put("ktp_list_param", ktpList);
                        // Pastikan parameter lain tidak ikut terkirim
                        params.put("ktp_param", null); 
                    }
                    break;
                case "job":
                    if(!kodePekerjaan.isEmpty()) {
                        params.put("kode_pekerjaan_param", kodePekerjaan);
                    }
                    break;
                case "room":
                    if(ruang != -1) {
                        params.put("ruang_param", ruang);
                    }
                    break;
            }
            

            // Ambil gambar sebagai InputStream
            InputStream ttdStream = getServletContext().getResourceAsStream("/reports/images/ttd_ceo.png");

            if (ttdStream == null) {
                throw new FileNotFoundException("Gambar TTD tidak ditemukan di path /reports/images/ttd_ceo.png");
            }

            // Parameter report
            params.put("TTD_IMAGE", ttdStream);
            
            // Compile dan isi report
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                reportPath, 
                params, 
                conn
            );
            
            // Export ke PDF
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
                "attachment; filename=\"Laporan_Karyawan_" + filterType + "_" + System.currentTimeMillis() + ".pdf\"");
            
            JasperExportManager.exportReportToPdfStream(jasperPrint, response.getOutputStream());
            
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Error generating report: " + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            try { if(conn != null) conn.close(); } catch (SQLException e) {}
        }
    }

    private List<String[]> getKaryawanList() {
        List<String[]> karyawanList = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT ktp, nama, kode_pekerjaan, ruang FROM tbkaryawan ORDER BY nama");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                karyawanList.add(new String[]{
                    rs.getString("ktp"),
                    rs.getString("nama"),
                    rs.getString("kode_pekerjaan"),
                    String.valueOf(rs.getInt("ruang"))
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return karyawanList;
    }

    private List<String[]> getPekerjaanList() {
        List<String[]> pekerjaanList = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // Query yang benar sesuai struktur tabel
            String sql = "SELECT kodepekerjaan, namapekerjaan FROM tbpekerjaan ORDER BY kodepekerjaan";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String kodePekerjaan = rs.getString("kodepekerjaan");
                    String namaPekerjaan = rs.getString("namapekerjaan");
                    pekerjaanList.add(new String[]{kodePekerjaan, namaPekerjaan});
                }
            }
            
            // Jika tbpekerjaan kosong, ambil dari tbkaryawan
            if (pekerjaanList.isEmpty()) {
                sql = "SELECT DISTINCT k.kode_pekerjaan, " +
                      "COALESCE(p.namapekerjaan, k.kode_pekerjaan) as namapekerjaan " +
                      "FROM tbkaryawan k " +
                      "LEFT JOIN tbpekerjaan p ON k.kode_pekerjaan = p.kodepekerjaan " +
                      "WHERE k.kode_pekerjaan IS NOT NULL " +
                      "ORDER BY k.kode_pekerjaan";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    while (rs.next()) {
                        String kodePekerjaan = rs.getString("kode_pekerjaan");
                        String namaPekerjaan = rs.getString("namapekerjaan");
                        pekerjaanList.add(new String[]{kodePekerjaan, namaPekerjaan});
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return pekerjaanList;
    }

    private List<Integer> getRuangList() {
        List<Integer> ruangList = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT DISTINCT ruang FROM tbkaryawan WHERE ruang IS NOT NULL ORDER BY ruang");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                ruangList.add(rs.getInt("ruang"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ruangList;
    }

    private String generateFilterUI(HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<String[]> karyawanList = getKaryawanList();
        List<String[]> pekerjaanList = getPekerjaanList();
        List<Integer> ruangList = getRuangList();
        
        StringBuilder karyawanOptions = new StringBuilder();
        for (String[] karyawan : karyawanList) {
            karyawanOptions.append("<option value='").append(karyawan[0]).append("'>")
                          .append(karyawan[1]).append(" (").append(karyawan[0]).append(")")
                          .append("</option>");
        }
        
        StringBuilder pekerjaanOptions = new StringBuilder();
        for (String[] pekerjaan : pekerjaanList) {
            pekerjaanOptions.append("<option value='").append(pekerjaan[0]).append("'>")
                           .append(pekerjaan[0]).append(" - ").append(pekerjaan[1])
                           .append("</option>");
        }
        
        StringBuilder ruangOptions = new StringBuilder();
        for (Integer ruang : ruangList) {
            ruangOptions.append("<option value='").append(ruang).append("'>")
                       .append("Ruang ").append(ruang)
                       .append("</option>");
        }

        StringBuilder html = new StringBuilder();
        
        // CSS Styling
        html.append("<style>")
           .append(".alert { padding: 1rem; margin-bottom: 1.5rem; border-radius: 8px; font-weight: 500; }")
           .append(".alert-success { background: linear-gradient(135deg, #d4edda, #c3e6cb); color: #155724; border: 1px solid #c3e6cb; }")
           .append(".alert-error { background: linear-gradient(135deg, #f8d7da, #f5c6cb); color: #721c24; border: 1px solid #f5c6cb; }")
           .append(".btn { padding: 0.75rem 1.5rem; border: none; border-radius: 6px; font-size: 1rem; font-weight: 500; cursor: pointer; transition: all 0.3s; text-decoration: none; display: inline-block; }")
           .append(".btn-primary { background: linear-gradient(135deg, var(--primary), #34495e); color: white; }")
           .append(".btn-primary:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(52, 73, 94, 0.3); }")
           .append(".btn-secondary { background: linear-gradient(135deg, var(--secondary), #2980b9); color: white; }")
           .append(".btn-secondary:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3); }")
           .append(".btn-success { background: linear-gradient(135deg, #27ae60, #229954); color: white; }")
           .append(".btn-success:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(39, 174, 96, 0.3); }")
           .append(".form-group { margin-bottom: 1.5rem; }")
           .append(".form-group label { display: block; margin-bottom: 0.5rem; font-weight: 600; color: var(--primary); }")
           .append(".form-group input, .form-group select { width: 100%; padding: 0.75rem; border: 2px solid #e9ecef; border-radius: 6px; font-size: 1rem; transition: all 0.3s; }")
           .append(".form-group input:focus, .form-group select:focus { outline: none; border-color: var(--secondary); box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1); }")
           .append(".page-header { background: linear-gradient(135deg, var(--primary), var(--secondary)); color: white; padding: 2rem; border-radius: 10px; margin-bottom: 2rem; text-align: center; }")
           .append(".page-header h1 { font-size: 2rem; margin-bottom: 0.5rem; }")
           .append(".page-header p { font-size: 1.1rem; opacity: 0.9; }")
           .append(".section-title { color: var(--primary); margin: 2rem 0 1rem 0; font-size: 1.5rem; font-weight: 600; display: flex; align-items: center; }")
           .append(".section-title i { margin-right: 0.5rem; color: var(--secondary); }")
           .append(".loading-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: none; z-index: 9999; }")
           .append(".loading-content { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); background: white; padding: 2rem; border-radius: 10px; text-align: center; }")
           .append(".spinner { border: 4px solid #f3f3f3; border-top: 4px solid var(--primary); border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; margin: 0 auto 1rem; }")
           .append("@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }")
           .append(".btn-download { position: relative; }")
           .append(".btn-download:disabled { opacity: 0.6; cursor: not-allowed; }")
                // Di bagian CSS tambahkan
            .append(".progress-bar {")
            .append("  width: 100%;")
            .append("  height: 8px;")
            .append("  background-color: #f0f0f0;")
            .append("  border-radius: 4px;")
            .append("  overflow: hidden;")
            .append("  margin: 1rem 0;")
            .append("  display: none;")
            .append("}")
            .append(".progress {")
            .append("  width: 0%;")
            .append("  height: 100%;")
            .append("  background: linear-gradient(90deg, var(--primary) 0%, var(--secondary) 100%);")
            .append("  transition: width 0.4s ease;")
            .append("}")
            .append(".print-loading .progress-bar { display: block; }")
           .append("</style>");

        // Page Header
        html.append("<div class='page-header'>")
           .append("<h1><i class='fas fa-file-pdf'></i> Laporan Data Karyawan</h1>")
           .append("<p>Pilih jenis laporan yang ingin dicetak</p>")
           .append("</div>");
        
        // Pesan notifikasi
        if(session.getAttribute("message") != null) {
            html.append("<div class='alert alert-success'>")
               .append("<i class='fas fa-check-circle'></i> ")
               .append(session.getAttribute("message"))
               .append("</div>");
            session.removeAttribute("message");
        }
        if(session.getAttribute("error") != null) {
            html.append("<div class='alert alert-error'>")
               .append("<i class='fas fa-exclamation-triangle'></i> ")
               .append(session.getAttribute("error"))
               .append("</div>");
            session.removeAttribute("error");
        }

        // Action Buttons Section
        html.append("<div class='card' style='margin-bottom: 2rem;'>")
           .append("<h2 class='section-title'><i class='fas fa-cogs'></i> Pilih Jenis Laporan</h2>")
           .append("<div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem;'>")
           
           // Tombol Semua Data
           .append("<form method='post' style='display: inline;'>")
           .append("<input type='hidden' name='action' value='show_all'>")
           .append("<button type='submit' class='btn btn-primary' style='width: 100%; height: 80px;'>")
           .append("<i class='fas fa-users' style='display: block; font-size: 1.5rem; margin-bottom: 0.5rem;'></i>")
           .append("Semua Data")
           .append("</button>")
           .append("</form>")
           
           // Tombol Data Tunggal
           .append("<form method='post' style='display: inline;'>")
           .append("<input type='hidden' name='action' value='show_single'>")
           .append("<button type='submit' class='btn btn-secondary' style='width: 100%; height: 80px;'>")
           .append("<i class='fas fa-user' style='display: block; font-size: 1.5rem; margin-bottom: 0.5rem;'></i>")
           .append("Data Tunggal")
           .append("</button>")
           .append("</form>")
           
           // Tombol By Pekerjaan
           .append("<form method='post' style='display: inline;'>")
           .append("<input type='hidden' name='action' value='show_job'>")
           .append("<button type='submit' class='btn btn-primary' style='width: 100%; height: 80px;'>")
           .append("<i class='fas fa-briefcase' style='display: block; font-size: 1.5rem; margin-bottom: 0.5rem;'></i>")
           .append("By Pekerjaan")
           .append("</button>")
           .append("</form>")
           
           // Tombol By Ruang
           .append("<form method='post' style='display: inline;'>")
           .append("<input type='hidden' name='action' value='show_room'>")
           .append("<button type='submit' class='btn btn-secondary' style='width: 100%; height: 80px;'>")
           .append("<i class='fas fa-door-open' style='display: block; font-size: 1.5rem; margin-bottom: 0.5rem;'></i>")
           .append("By Ruang")
           .append("</button>")
           .append("</form>")
           
           .append("</div>")
           .append("</div>");

        // Form berdasarkan action yang dipilih
        String currentAction = (String) session.getAttribute("current_action");
        
        if("show_all".equals(currentAction)) {
            html.append("<div class='card' id='form-section'>")
               .append("<h2 class='section-title'><i class='fas fa-users'></i> Cetak Semua Data Karyawan</h2>")
               .append("<p style='color:#666;margin-bottom:1.5rem;'>Laporan akan mencakup seluruh data karyawan yang terdaftar dalam sistem (").append(karyawanList.size()).append(" karyawan)</p>")
               .append("<form method='post' onsubmit='showDownloadLoading(this)'>")
               .append("<input type='hidden' name='filter_type' value='all'>")
               .append("<button type='submit' name='action' value='cetak' class='btn btn-primary btn-download'>")
               .append("<i class='fas fa-print'></i> Cetak Semua Data")
               .append("</button>")
               .append("</form>")
               .append("</div>");
        }
        
        else if("show_single".equals(currentAction)) {
            html.append("<div class='card' id='form-section'>")
               .append("<h2 class='section-title'><i class='fas fa-user'></i> Cetak Data Karyawan Tunggal</h2>");
            
            if(karyawanList.isEmpty()) {
                html.append("<div class='alert alert-error'>")
                   .append("<i class='fas fa-exclamation-triangle'></i> Tidak ada data karyawan tersedia")
                   .append("</div>");
            } else {
                html.append("<form method='post' onsubmit='showDownloadLoading(this)'>")
                   .append("<input type='hidden' name='filter_type' value='single'>")
                   .append("<div class='form-group'>")
                   .append("<label><i class='fas fa-user'></i> Pilih Karyawan:</label>")
                   .append("<select name='selected_ktp' required>")
                   .append("<option value=''>-- Pilih Karyawan --</option>")
                   .append(karyawanOptions.toString())
                   .append("</select>")
                   .append("</div>")
                   .append("<button type='submit' name='action' value='cetak' class='btn btn-primary btn-download'>")
                   .append("<i class='fas fa-print'></i> Cetak Data")
                   .append("</button>")
                   .append("</form>");
            }
            html.append("</div>");
        }
        
        else if("show_multiple".equals(currentAction)) {
            html.append("<div class='card' id='form-section'>")
               .append("<h2 class='section-title'><i class='fas fa-user-friends'></i> Cetak Multiple Data Karyawan</h2>");
            
            if(karyawanList.isEmpty()) {
                html.append("<div class='alert alert-error'>")
                   .append("<i class='fas fa-exclamation-triangle'></i> Tidak ada data karyawan tersedia")
                   .append("</div>");
            } else {
                html.append("<form method='post' onsubmit='showDownloadLoading(this)'>")
                   .append("<input type='hidden' name='filter_type' value='multiple'>")
                   .append("<div class='form-group'>")
                   .append("<label><i class='fas fa-users'></i> Pilih Karyawan (Ctrl+Click untuk multiple):</label>")
                   .append("<select name='selected_ktp' multiple size='6' required style='height: 150px;'>")
                   .append(karyawanOptions.toString())
                   .append("</select>")
                   .append("<small style='color:#666;font-style:italic;'>Tahan Ctrl dan klik untuk memilih beberapa karyawan</small>")
                   .append("</div>")
                   .append("<button type='submit' name='action' value='cetak' class='btn btn-primary btn-download'>")
                   .append("<i class='fas fa-print'></i> Cetak Data")
                   .append("</button>")
                   .append("</form>");
            }
            html.append("</div>");
        }
        
        else if("show_job".equals(currentAction)) {
            html.append("<div class='card' id='form-section'>")
               .append("<h2 class='section-title'><i class='fas fa-briefcase'></i> Cetak Data Berdasarkan Pekerjaan</h2>");
            
            if(pekerjaanList.isEmpty()) {
                html.append("<div class='alert alert-error'>")
                   .append("<i class='fas fa-exclamation-triangle'></i> Tidak ada data pekerjaan tersedia. ")
                   .append("Pastikan tabel tbpekerjaan memiliki data atau karyawan memiliki kode_pekerjaan.")
                   .append("</div>");
            } else {
                html.append("<p style='color:#666;margin-bottom:1.5rem;'>")
                   .append("Ditemukan ").append(pekerjaanList.size()).append(" jenis pekerjaan")
                   .append("</p>")
                   .append("<form method='post' onsubmit='showDownloadLoading(this)'>")
                   .append("<input type='hidden' name='filter_type' value='job'>")
                   .append("<div class='form-group'>")
                   .append("<label><i class='fas fa-briefcase'></i> Pilih Pekerjaan:</label>")
                   .append("<select name='kode_pekerjaan' required>")
                   .append("<option value=''>-- Pilih Pekerjaan --</option>")
                   .append(pekerjaanOptions.toString())
                   .append("</select>")
                   .append("</div>")
                   .append("<button type='submit' name='action' value='cetak' class='btn btn-primary btn-download'>")
                   .append("<i class='fas fa-print'></i> Cetak Data")
                   .append("</button>")
                   .append("</form>");
            }
            html.append("</div>");
        }
        
        else if("show_room".equals(currentAction)) {
            html.append("<div class='card' id='form-section'>")
               .append("<h2 class='section-title'><i class='fas fa-door-open'></i> Cetak Data Berdasarkan Ruang</h2>");
            
            if(ruangList.isEmpty()) {
                html.append("<div class='alert alert-error'>")
                   .append("<i class='fas fa-exclamation-triangle'></i> Tidak ada data ruang tersedia. ")
                   .append("Pastikan karyawan memiliki data ruang.")
                   .append("</div>");
            } else {
                html.append("<p style='color:#666;margin-bottom:1.5rem;'>")
                   .append("Ruang yang tersedia: ");
                for(int i = 0; i < ruangList.size(); i++) {
                    if(i > 0) html.append(", ");
                    html.append("Ruang ").append(ruangList.get(i));
                }
                html.append("</p>")
                   .append("<form method='post' onsubmit='showDownloadLoading(this)'>")
                   .append("<input type='hidden' name='filter_type' value='room'>")
                   .append("<div class='form-group'>")
                   .append("<label><i class='fas fa-door-open'></i> Pilih Ruang:</label>")
                   .append("<select name='ruang' required>")
                   .append("<option value=''>-- Pilih Ruang --</option>")
                   .append(ruangOptions.toString())
                   .append("</select>")
                   .append("</div>")
                   .append("<button type='submit' name='action' value='cetak' class='btn btn-primary btn-download'>")
                   .append("<i class='fas fa-print'></i> Cetak Data")
                   .append("</button>")
                   .append("</form>");
            }
            html.append("</div>");
        }

        // Loading Overlay (tidak diperlukan lagi untuk navigation)
        // html.append("<div class='loading-overlay' id='loadingOverlay'>")
        //    .append("<div class='loading-content'>")
        //    .append("<div class='spinner'></div>")
        //    .append("<h3 id='loadingText'>Memproses...</h3>")
        //    .append("<p id='loadingSubtext'>Mohon tunggu sebentar</p>")
        //    .append("</div>")
        //    .append("</div>");

        // JavaScript untuk auto-scroll dan loading yang diperbaiki
        html.append("<script>")
   .append("function showDownloadLoading(form) {")
   .append("  var button = form.querySelector('button[type=\"submit\"]');")
   .append("  var originalText = button.innerHTML;")
   .append("  button.disabled = true;")
   .append("  button.innerHTML = '<i class=\"fas fa-spinner fa-spin\"></i> Generating PDF...';")
   .append("  ")
   .append("  // Reset button setelah 5 detik (estimasi waktu download)")
   .append("  setTimeout(function() {")
   .append("    button.disabled = false;")
   .append("    button.innerHTML = originalText;")
   .append("  }, 5000);")
   .append("  ")
   .append("  return true;")
   .append("}")
   .append("function scrollToForm() {")
   .append("  var formSection = document.getElementById('form-section');")
   .append("  if (formSection) {")
   .append("    formSection.scrollIntoView({ behavior: 'smooth', block: 'start' });")
   .append("  }")
   .append("}")
   .append("window.addEventListener('load', function() {")
   .append("  // Check if there's a form section to scroll to")
   .append("  var formSection = document.getElementById('form-section');")
   .append("  if (formSection) {")
   .append("    // Immediate scroll without delay")
   .append("    setTimeout(scrollToForm, 100);")
   .append("  }")
   .append("});")
   .append("</script>");

        // Clear session action
        session.removeAttribute("current_action");
        
        return html.toString();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setAttribute("activeMenu", "laporan");
        request.setAttribute("konten", generateFilterUI(request));
        
        RequestDispatcher dispatcher = request.getRequestDispatcher("/MainForm");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String action = request.getParameter("action");
        
        switch(action) {
            case "show_all":
            case "show_single":
            case "show_multiple":
            case "show_job":
            case "show_room":
                session.setAttribute("current_action", action);
                response.sendRedirect("LaporanKaryawan");
                return;
                
            case "cetak":
                String filterType = request.getParameter("filter_type");
                String[] selectedKtp = request.getParameterValues("selected_ktp");
                String kodePekerjaan = request.getParameter("kode_pekerjaan") != null ? 
                    request.getParameter("kode_pekerjaan") : "";
                int ruang = -1;
                
                try {
                    String ruangParam = request.getParameter("ruang");
                    if (ruangParam != null && !ruangParam.isEmpty()) {
                        ruang = Integer.parseInt(ruangParam);
                    }
                } catch (NumberFormatException e) {
                    ruang = -1;
                }
                
                generateReport(request, response, selectedKtp, kodePekerjaan, ruang, filterType);
                return;
        }
        
        response.sendRedirect("LaporanKaryawan");
    }
}