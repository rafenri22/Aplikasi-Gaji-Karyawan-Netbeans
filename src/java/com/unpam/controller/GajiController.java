package com.unpam.controller;

import com.unpam.model.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

/**
 *
 * @author rafky
 */
@WebServlet(name = "GajiController", urlPatterns = {"/GajiController"})
public class GajiController extends HttpServlet {
    private final Gaji gajiModel = new Gaji();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        String action = request.getParameter("action");
        String searchQuery = request.getParameter("search");
        
        try {
            if("edit".equals(action)) {
                session.setAttribute("editKtp", request.getParameter("ktp"));
                session.setAttribute("editKode", request.getParameter("kode"));
            } else if("cancelEdit".equals(action)) {
                session.removeAttribute("editKtp");
                session.removeAttribute("editKode");
            }
            
            List<Object[]> allGaji = getAllGajiData(searchQuery);
            session.setAttribute("listGaji", allGaji.toArray(new Object[0][]));
        } catch (Exception e) {
            session.setAttribute("error", "Error: " + e.getMessage());
        }
        
        String htmlContent = generateGajiForm(request);
        request.setAttribute("konten", htmlContent);
        // Set active menu
        request.setAttribute("activeMenu", "transaksi");
        request.getRequestDispatcher("/MainForm").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        String action = request.getParameter("action");
        
        if("update".equals(action)) {
            processUpdateGaji(request, session);
        } else if("hapus".equals(action)) {
            processHapusGaji(request, session);
        }
        
        // Redirect dengan anchor ke data table untuk tetap di posisi yang sama
        response.sendRedirect("GajiController#dataTable");
    }

    // Helper method untuk format mata uang yang lebih ringkas
    private String formatCurrency(double amount) {
        if (amount >= 1000000000) {
            return String.format("%.1f M", amount / 1000000000);
        } else if (amount >= 1000000) {
            return String.format("%.1f Jt", amount / 1000000);
        } else if (amount >= 1000) {
            return String.format("%.0f Rb", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    private String generateGajiForm(HttpServletRequest request) {
        HttpSession session = request.getSession();
        StringBuilder html = new StringBuilder();
        
        // Enhanced CSS Styling - Matching MainForm.java
        html.append("<style>")
           // Use same CSS variables as MainForm
           .append(":root { --primary: #2c3e50; --secondary: #3498db; --light: #ecf0f1; }")
           .append("* { box-sizing: border-box; }")
           
           // Alert styles matching MainForm color scheme
           .append(".alert { padding: 1rem; margin-bottom: 1.5rem; border-radius: 10px; font-weight: 500; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }")
           .append(".alert-success { background: linear-gradient(135deg, #d4edda, #c3e6cb); color: #155724; border: 1px solid #c3e6cb; }")
           .append(".alert-error { background: linear-gradient(135deg, #f8d7da, #f5c6cb); color: #721c24; border: 1px solid #f5c6cb; }")
           
           // Button styles matching MainForm
           .append(".btn { padding: 0.75rem 1.5rem; border: none; border-radius: 5px; font-size: 1rem; font-weight: 500; cursor: pointer; transition: all 0.3s; text-decoration: none; display: inline-block; font-family: 'Segoe UI', sans-serif; }")
           .append(".btn-primary { background: var(--primary); color: white; }")
           .append(".btn-primary:hover { background: #34495e; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(44, 62, 80, 0.3); }")
           .append(".btn-secondary { background: var(--secondary); color: white; }")
           .append(".btn-secondary:hover { background: #2980b9; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3); }")
           .append(".btn-danger { background: #e74c3c; color: white; }")
           .append(".btn-danger:hover { background: #c0392b; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(231, 76, 60, 0.3); }")
           .append(".btn-success { background: #27ae60; color: white; }")
           .append(".btn-success:hover { background: #229954; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(39, 174, 96, 0.3); }")
           .append(".btn-edit { background: #f39c12; color: white; }")
           .append(".btn-edit:hover { background: #e67e22; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(243, 156, 18, 0.3); }")
           .append(".btn-small { padding: 0.5rem 1rem; font-size: 0.9rem; }")
           .append(".btn-warning { background: #f39c12; color: white; }")
           .append(".btn-warning:hover { background: #e67e22; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(243, 156, 18, 0.3); }")
           
           // Table styles matching MainForm card design
           .append(".data-table { width: 100%; border-collapse: collapse; margin: 1.5rem 0; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }")
           .append(".data-table thead { background: var(--primary); color: white; }")
           .append(".data-table th, .data-table td { padding: 1rem; text-align: left; border-bottom: 1px solid #eee; }")
           .append(".data-table tbody tr:hover { background: var(--light); transition: all 0.3s; }")
           .append(".data-table tbody tr:nth-child(even) { background: #f8f9fa; }")
           .append(".data-table tbody tr.editing { background: linear-gradient(135deg, #fff3cd, #ffeaa7); }")
           
           // Search container matching MainForm card style
           .append(".search-container { background: white; padding: 2rem; border-radius: 10px; margin-bottom: 2rem; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }")
           .append(".search-box { display: flex; gap: 1rem; align-items: center; flex-wrap: wrap; }")
           .append(".search-input { flex: 1; min-width: 300px; padding: 0.75rem; border: 2px solid #e9ecef; border-radius: 5px; font-size: 1rem; transition: all 0.3s; font-family: 'Segoe UI', sans-serif; }")
           .append(".search-input:focus { outline: none; border-color: var(--secondary); box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1); }")
           
           // Page header matching MainForm hero style
           .append(".page-header { background: linear-gradient(135deg, var(--primary), var(--secondary)); color: white; padding: 4rem; border-radius: 10px; margin-bottom: 2rem; text-align: center; text-shadow: 2px 2px 4px rgba(0,0,0,0.3); }")
           .append(".page-header h1 { font-size: 2.5em; margin-bottom: 1rem; font-family: 'Segoe UI', sans-serif; }")
           .append(".page-header p { font-size: 1.2em; opacity: 0.9; }")
           
           // Section titles matching MainForm
           .append(".section-title { color: var(--primary); margin: 2rem 0 1rem 0; font-size: 1.5rem; font-weight: 600; display: flex; align-items: center; font-family: 'Segoe UI', sans-serif; }")
           .append(".section-title i { margin-right: 0.5rem; color: var(--secondary); }")
           
           // Stats container matching MainForm feature cards
           .append(".stats-container { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1.5rem; margin-bottom: 2rem; }")
           .append(".stats-card { background: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); padding: 2rem; text-align: center; transition: all 0.3s; min-height: 120px; display: flex; flex-direction: column; justify-content: center; }")
           .append(".stats-card:hover { transform: translateY(-5px); box-shadow: 0 4px 15px rgba(0,0,0,0.15); }")
           .append(".stats-number { font-size: 2.5em; font-weight: bold; color: var(--secondary); word-wrap: break-word; overflow-wrap: break-word; line-height: 1.2; }")
           .append(".stats-label { color: #6c757d; font-size: 0.9rem; margin-top: 0.5rem; }")
           .append(".salary-amount { font-weight: bold; color: var(--secondary); }")
           
           // Action buttons
           .append(".action-buttons { display: flex; gap: 0.5rem; flex-wrap: wrap; }")
           .append(".edit-form { background: linear-gradient(135deg, #fff3cd, #ffeaa7); padding: 0.5rem; border-radius: 5px; }")
           .append(".edit-input { width: 100px; padding: 0.5rem; border: 1px solid #ddd; border-radius: 5px; font-size: 0.9rem; font-family: 'Segoe UI', sans-serif; }")
           
           // No data state
           .append(".no-data { text-align: center; padding: 3rem; color: #6c757d; }")
           .append(".no-data i { font-size: 3rem; margin-bottom: 1rem; color: #dee2e6; }")
           
           // Quick Access matching MainForm feature card style
           .append(".quick-access { background: white; padding: 2rem; border-radius: 10px; margin-bottom: 2rem; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }")
           .append(".quick-access-buttons { display: flex; gap: 1rem; flex-wrap: wrap; justify-content: center; }")
           
           // Notification styles
           .append(".notification-area { margin-bottom: 1rem; }")
           .append(".alert-floating { animation: slideInDown 0.5s ease-out; }")
           .append("@keyframes slideInDown { from { transform: translateY(-100%); opacity: 0; } to { transform: translateY(0); opacity: 1; } }")
           
           // Responsive design matching MainForm
           .append("@media (max-width: 768px) {")
           .append("  .stats-container { grid-template-columns: repeat(2, 1fr); }")
           .append("  .stats-number { font-size: 1.8rem; }")
           .append("  .quick-access-buttons { flex-direction: column; }")
           .append("  .page-header { padding: 2rem; }")
           .append("  .page-header h1 { font-size: 2rem; }")
           .append("  .search-box { flex-direction: column; }")
           .append("  .search-input { min-width: 100%; }")
           .append("}")
           .append("@media (max-width: 480px) {")
           .append("  .stats-container { grid-template-columns: 1fr; }")
           .append("  .stats-number { font-size: 1.6rem; }")
           .append("  .page-header h1 { font-size: 1.5rem; }")
           .append("  .action-buttons { flex-direction: column; }")
           .append("}")
           .append("</style>");

        // Page Header matching MainForm hero style
        html.append("<div class='page-header'>")
           .append("<h1><i class='fas fa-calculator'></i> Sistem Penggajian Karyawan</h1>")
           .append("<p>Sistem Informasi Terintegrasi untuk Manajemen Penggajian dan Tunjangan Karyawan</p>")
           .append("</div>");

        // Quick Access Section matching MainForm feature cards
        html.append("<div class='card'>")
           .append("<h2 class='section-title'><i class='fas fa-rocket'></i> Akses Cepat</h2>")
           .append("<div class='quick-access'>")
           .append("<div class='quick-access-buttons'>")
           .append("<button onclick='scrollToSearch()' class='btn btn-primary'><i class='fas fa-search'></i> Ke Pencarian</button>")
           .append("<button onclick='scrollToDataTable()' class='btn btn-warning'><i class='fas fa-table'></i> Ke Data Penggajian</button>")
           .append("<button onclick='refreshData()' class='btn btn-secondary'><i class='fas fa-sync-alt'></i> Refresh Data</button>")
           .append("</div>")
           .append("</div>")
           .append("</div>");
        
        // Alert Messages
        if(session.getAttribute("error") != null) {
            html.append("<div class='alert alert-error alert-floating'>")
               .append("<i class='fas fa-exclamation-triangle'></i> ")
               .append(session.getAttribute("error"))
               .append("</div>");
            session.removeAttribute("error");
        }

        // Statistics Cards matching MainForm
        if(session.getAttribute("listGaji") != null) {
            Object[][] listGaji = (Object[][]) session.getAttribute("listGaji");
            double totalGajiBersih = 0;
            double totalGajiKotor = 0;
            double totalTunjangan = 0;
            
            for(Object[] row : listGaji) {
                totalGajiBersih += (Double) row[2];
                totalGajiKotor += (Double) row[3];
                totalTunjangan += (Double) row[4];
            }
            
            html.append("<div class='stats-container'>")
               .append("<div class='stats-card'>")
               .append("<h2 style='color:var(--primary);margin-bottom:1rem;'><i class='fas fa-users'></i> Total Karyawan</h2>")
               .append("<p class='stats-number'>").append(listGaji.length).append("</p>")
               .append("</div>")
               .append("<div class='stats-card'>")
               .append("<h2 style='color:var(--primary);margin-bottom:1rem;'><i class='fas fa-money-bill-wave'></i> Total Gaji Bersih</h2>")
               .append("<p class='stats-number'>Rp ").append(formatCurrency(totalGajiBersih)).append("</p>")
               .append("</div>")
               .append("<div class='stats-card'>")
               .append("<h2 style='color:var(--primary);margin-bottom:1rem;'><i class='fas fa-coins'></i> Total Gaji Kotor</h2>")
               .append("<p class='stats-number'>Rp ").append(formatCurrency(totalGajiKotor)).append("</p>")
               .append("</div>")
               .append("<div class='stats-card'>")
               .append("<h2 style='color:var(--primary);margin-bottom:1rem;'><i class='fas fa-gift'></i> Total Tunjangan</h2>")
               .append("<p class='stats-number'>Rp ").append(formatCurrency(totalTunjangan)).append("</p>")
               .append("</div>")
               .append("</div>");
        }

        // Search Section matching MainForm card style
        html.append("<div class='card' id='searchSection'>")
           .append("<h2 class='section-title'><i class='fas fa-search'></i> Pencarian Data</h2>")
           .append("<div class='search-container'>")
           .append("<div class='search-box'>")
           .append("<input type='text' id='searchInput' class='search-input' placeholder='🔍 Cari berdasarkan nama karyawan...' value='")
           .append(request.getParameter("search") != null ? request.getParameter("search") : "")
           .append("'>")
           .append("<button onclick='performSearch()' class='btn btn-primary'><i class='fas fa-search'></i> Cari</button>")
           .append("<button onclick='clearSearch()' class='btn btn-secondary'><i class='fas fa-times'></i> Reset</button>")
           .append("</div>")
           .append("</div>")
           .append("</div>");

        // Data Table Section
        html.append("<div class='card' id='dataTable'>")
           .append("<h2 class='section-title'><i class='fas fa-table'></i> Data Penggajian Karyawan</h2>");

        // Notification area untuk aksi
        html.append("<div class='notification-area'>");
        if(session.getAttribute("message") != null) {
            html.append("<div class='alert alert-success alert-floating'>")
               .append("<i class='fas fa-check-circle'></i> ")
               .append(session.getAttribute("message"))
               .append("</div>");
            session.removeAttribute("message");
        }
        html.append("</div>");

        if(session.getAttribute("listGaji") != null) {
            Object[][] listGaji = (Object[][]) session.getAttribute("listGaji");
            String editKtp = (String) session.getAttribute("editKtp");
            String editKode = (String) session.getAttribute("editKode");
            
            if(listGaji.length > 0) {
                html.append("<div style='overflow-x: auto;'>")
                   .append("<table class='data-table' id='gajiTable'>")
                   .append("<thead>")
                   .append("<tr>")
                   .append("<th><i class='fas fa-user'></i> Nama Karyawan</th>")
                   .append("<th><i class='fas fa-briefcase'></i> Pekerjaan</th>")
                   .append("<th><i class='fas fa-money-bill-wave'></i> Gaji Bersih</th>")
                   .append("<th><i class='fas fa-coins'></i> Gaji Kotor</th>")
                   .append("<th><i class='fas fa-gift'></i> Tunjangan</th>")
                   .append("<th><i class='fas fa-tools'></i> Aksi</th>")
                   .append("</tr>")
                   .append("</thead>")
                   .append("<tbody>");

                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
                
                for(Object[] row : listGaji) {
                    String currentKtp = (String) row[5];
                    String currentKode = (String) row[6];
                    boolean isEditing = currentKtp.equals(editKtp) && currentKode.equals(editKode);
                    
                    html.append("<tr").append(isEditing ? " class='editing'" : "").append(" data-nama='").append(row[0].toString().toLowerCase()).append("'>");
                    
                    if(isEditing) {
                        html.append("<form method='post' class='edit-form'>")
                           .append("<input type='hidden' name='action' value='update'>")
                           .append("<input type='hidden' name='ktp' value='").append(currentKtp).append("'>")
                           .append("<input type='hidden' name='kode' value='").append(currentKode).append("'>")
                           .append("<td><strong>").append(row[0]).append("</strong></td>")
                           .append("<td>").append(row[1]).append("</td>")
                           .append("<td><input type='number' name='bersih' value='").append(row[2]).append("' step='0.01' class='edit-input' placeholder='Gaji Bersih'></td>")
                           .append("<td><input type='number' name='kotor' value='").append(row[3]).append("' step='0.01' class='edit-input' placeholder='Gaji Kotor'></td>")
                           .append("<td><input type='number' name='tunjangan' value='").append(row[4]).append("' step='0.01' class='edit-input' placeholder='Tunjangan'></td>")
                           .append("<td>")
                           .append("<div class='action-buttons'>")
                           .append("<button type='submit' class='btn btn-success btn-small'><i class='fas fa-save'></i> Simpan</button>")
                           .append("<a href='GajiController?action=cancelEdit#dataTable' class='btn btn-secondary btn-small'><i class='fas fa-times'></i> Batal</a>")
                           .append("</div>")
                           .append("</td>")
                           .append("</form>");
                    } else {
                        html.append("<td><strong>").append(row[0]).append("</strong></td>")
                           .append("<td>").append(row[1]).append("</td>")
                           .append("<td><span class='salary-amount'>").append(currencyFormat.format((Double) row[2])).append("</span></td>")
                           .append("<td><span class='salary-amount'>").append(currencyFormat.format((Double) row[3])).append("</span></td>")
                           .append("<td><span class='salary-amount'>").append(currencyFormat.format((Double) row[4])).append("</span></td>")
                           .append("<td>")
                           .append("<div class='action-buttons'>")
                           .append("<a href='GajiController?action=edit&ktp=").append(currentKtp)
                           .append("&kode=").append(currentKode).append("#dataTable' class='btn btn-edit btn-small'><i class='fas fa-edit'></i> Edit</a>")
                           .append("<form method='post' style='display:inline;' onsubmit='return confirm(\"Yakin ingin mereset gaji ini ke 0?\")'>")
                           .append("<input type='hidden' name='action' value='hapus'>")
                           .append("<input type='hidden' name='ktp' value='").append(currentKtp).append("'>")
                           .append("<input type='hidden' name='kode' value='").append(currentKode).append("'>")
                           .append("<button type='submit' class='btn btn-danger btn-small'><i class='fas fa-trash'></i> Reset</button>")
                           .append("</form>")
                           .append("</div>")
                           .append("</td>");
                    }
                    html.append("</tr>");
                }
                html.append("</tbody></table>")
                   .append("</div>");
            } else {
                html.append("<div class='no-data'>")
                   .append("<i class='fas fa-inbox'></i>")
                   .append("<h3>Tidak ada data gaji</h3>")
                   .append("<p>Belum ada data gaji yang tersedia atau sesuai dengan pencarian Anda.</p>")
                   .append("</div>");
            }
        }
        
        html.append("</div>");

        // JavaScript untuk interaktivitas
        html.append("<script>")
           // Quick access functions
           .append("function scrollToSearch() {")
           .append("  document.getElementById('searchSection').scrollIntoView({ behavior: 'smooth' });")
           .append("  setTimeout(() => document.getElementById('searchInput').focus(), 500);")
           .append("}")
           .append("function scrollToDataTable() {")
           .append("  document.getElementById('dataTable').scrollIntoView({ behavior: 'smooth' });")
           .append("}")
           .append("function refreshData() {")
           .append("  window.location.href = 'GajiController';")
           .append("}")
           // Search functions
           .append("function performSearch() {")
           .append("  var searchValue = document.getElementById('searchInput').value;")
           .append("  window.location.href = 'GajiController?search=' + encodeURIComponent(searchValue) + '#dataTable';")
           .append("}")
           .append("function clearSearch() {")
           .append("  window.location.href = 'GajiController#dataTable';")
           .append("}")
           .append("function filterTable() {")
           .append("  var input = document.getElementById('searchInput');")
           .append("  var filter = input.value.toLowerCase();")
           .append("  var table = document.getElementById('gajiTable');")
           .append("  if (!table) return;")
           .append("  var tr = table.getElementsByTagName('tr');")
           .append("  for (var i = 1; i < tr.length; i++) {")
           .append("    var td = tr[i].getElementsByTagName('td')[0];")
           .append("    if (td) {")
           .append("      var txtValue = td.textContent || td.innerText;")
           .append("      if (txtValue.toLowerCase().indexOf(filter) > -1) {")
           .append("        tr[i].style.display = '';")
           .append("      } else {")
           .append("        tr[i].style.display = 'none';")
           .append("      }")
           .append("    }")
           .append("  }")
           .append("}")
           // Event listeners
           .append("document.getElementById('searchInput').addEventListener('keypress', function(e) {")
           .append("  if (e.key === 'Enter') {")
           .append("    performSearch();")
           .append("  }")
           .append("});")
           .append("document.getElementById('searchInput').addEventListener('keyup', filterTable);")
           // Auto scroll to anchor if present
           .append("window.addEventListener('load', function() {")
           .append("  if (window.location.hash) {")
           .append("    setTimeout(() => {")
           .append("      document.querySelector(window.location.hash).scrollIntoView({ behavior: 'smooth' });")
           .append("    }, 100);")
           .append("  }")
           .append("});")
           .append("</script>");
        
        return html.toString();
    }

    private List<Object[]> getAllGajiData(String searchQuery) throws Exception {
        List<Object[]> allData = new ArrayList<>();
        Connection connection = null;
        try {
            connection = gajiModel.koneksi.getConnection();
            String sql = "SELECT k.nama, p.namapekerjaan, g.gajibersih, g.gajikotor, g.tunjangan, g.ktp, g.kodepekerjaan " +
                         "FROM tbgaji g " +
                         "JOIN tbkaryawan k ON g.ktp = k.ktp " +
                         "JOIN tbpekerjaan p ON g.kodepekerjaan = p.kodepekerjaan " +
                         "WHERE LOWER(k.nama) LIKE ? " +
                         "ORDER BY k.nama";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, "%" + (searchQuery != null ? searchQuery.toLowerCase() : "") + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Object[] row = {
                            rs.getString("nama"),
                            rs.getString("namapekerjaan"),
                            rs.getDouble("gajibersih"),
                            rs.getDouble("gajikotor"),
                            rs.getDouble("tunjangan"),
                            rs.getString("ktp"),
                            rs.getString("kodepekerjaan")
                        };
                        allData.add(row);
                    }
                }
            }
        } finally {
            if (connection != null) connection.close();
        }
        return allData;
    }

    private void processUpdateGaji(HttpServletRequest request, HttpSession session) {
        try {
            String ktp = request.getParameter("ktp");
            String kode = request.getParameter("kode");
            double bersih = Double.parseDouble(request.getParameter("bersih"));
            double kotor = Double.parseDouble(request.getParameter("kotor"));
            double tunjangan = Double.parseDouble(request.getParameter("tunjangan"));
            
            Connection connection = null;
            try {
                connection = gajiModel.koneksi.getConnection();
                String sql = "UPDATE tbgaji SET gajibersih=?, gajikotor=?, tunjangan=? WHERE ktp=? AND kodepekerjaan=?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setDouble(1, bersih);
                    pstmt.setDouble(2, kotor);
                    pstmt.setDouble(3, tunjangan);
                    pstmt.setString(4, ktp);
                    pstmt.setString(5, kode);
                    pstmt.executeUpdate();
                    session.setAttribute("message", "Data gaji berhasil diperbarui");
                    session.removeAttribute("editKtp");
                    session.removeAttribute("editKode");
                }
            } finally {
                if (connection != null) connection.close();
            }
        } catch (Exception e) {
            session.setAttribute("error", "Error: " + e.getMessage());
        }
    }

    private void processHapusGaji(HttpServletRequest request, HttpSession session) {
        try {
            String ktp = request.getParameter("ktp");
            String kode = request.getParameter("kode");
            
            Connection connection = null;
            try {
                connection = gajiModel.koneksi.getConnection();
                String sql = "UPDATE tbgaji SET gajibersih=0, gajikotor=0, tunjangan=0 WHERE ktp=? AND kodepekerjaan=?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, ktp);
                    pstmt.setString(2, kode);
                    pstmt.executeUpdate();
                    session.setAttribute("message", "Data gaji berhasil direset ke 0");
                }
            } finally {
                if (connection != null) connection.close();
            }
        } catch (Exception e) {
            session.setAttribute("error", "Error: " + e.getMessage());
        }
    }
}