/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unpam.view;

import com.unpam.model.Koneksi;
import java.io.*;
import java.sql.*;
import java.util.HashMap;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import net.sf.jasperreports.engine.*;

/**
 *
 * @author rafky
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

@WebServlet(name = "LaporanPekerjaan", urlPatterns = {"/LaporanPekerjaan"})
public class LaporanPekerjaan extends HttpServlet {

    private Connection getConnection() throws SQLException {
        Koneksi koneksi = new Koneksi();
        Connection conn = koneksi.getConnection();
        if (conn == null) {
            throw new SQLException("Gagal koneksi: " + koneksi.getPesanKesalahan());
        }
        return conn;
    }
    
    private String generateFilterUI(HttpServletRequest request) {
        HttpSession session = request.getSession();
        StringBuilder html = new StringBuilder();

        // CSS Styling
        html.append("<style>")
           .append(".alert { padding: 1rem; margin-bottom: 1.5rem; border-radius: 8px; font-weight: 500; }")
           .append(".alert-error { background: linear-gradient(135deg, #f8d7da, #f5c6cb); color: #721c24; border: 1px solid #f5c6cb; }")
           .append(".btn { padding: 0.75rem 1.5rem; border: none; border-radius: 6px; font-size: 1rem; font-weight: 500; cursor: pointer; transition: all 0.3s; text-decoration: none; display: inline-block; }")
           .append(".btn-primary { background: linear-gradient(135deg, var(--primary), #34495e); color: white; }")
           .append(".btn-primary:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(52, 73, 94, 0.3); }")
           .append(".form-group { margin-bottom: 1.5rem; }")
           .append(".page-header { background: linear-gradient(135deg, var(--primary), var(--secondary)); color: white; padding: 2rem; border-radius: 10px; margin-bottom: 2rem; text-align: center; }")
           .append(".page-header h1 { font-size: 2rem; margin-bottom: 0.5rem; }")
           .append(".page-header p { font-size: 1.1rem; opacity: 0.9; }")
           .append(".card { max-width: 800px; margin: 2rem auto; padding: 2rem; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }")
           .append("</style>");

        // Page Header
        html.append("<div class='page-header'>")
           .append("<h1><i class='fas fa-file-pdf'></i> Laporan Data Pekerjaan</h1>")
           .append("<p>Cetak laporan data pekerjaan yang terdaftar dalam sistem</p>")
           .append("</div>");
        
        // Error messages
        if(session.getAttribute("error") != null) {
            html.append("<div class='alert alert-error'>")
               .append("<i class='fas fa-exclamation-triangle'></i> ")
               .append(session.getAttribute("error"))
               .append("</div>");
            session.removeAttribute("error");
        }

        // Form
        html.append("<div class='card'>")
           .append("<form method='post' onsubmit='showLoading(this)'>")
           .append("<p style='color:#666;margin-bottom:1.5rem;'>Klik tombol di bawah untuk menghasilkan laporan pekerjaan lengkap</p>")
           .append("<button type='submit' name='action' value='cetak' class='btn btn-primary'>")
           .append("<i class='fas fa-print'></i> Cetak Laporan")
           .append("</button>")
           .append("</form>")
           .append("</div>");

        // JavaScript
        html.append("<script>")
           .append("function showLoading(form) {")
           .append("  var button = form.querySelector('button');")
           .append("  button.innerHTML = '<i class=\"fas fa-spinner fa-spin\"></i> Membuat Laporan...';")
           .append("  button.disabled = true;")
           .append("  setTimeout(function(){ button.disabled = false; button.innerHTML = '<i class=\"fas fa-print\"></i> Cetak Laporan'; }, 5000);")
           .append("}")
           .append("</script>");

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
        
        Connection conn = null;
        try {
            conn = getConnection();
            
            // Path ke file .jasper
            String reportPath = getServletContext().getRealPath("/reports/laporan_pekerjaan.jasper");
            
            // Ambil gambar TTD
            InputStream ttdStream = getServletContext().getResourceAsStream("/reports/images/ttd_ceo.png");
            if (ttdStream == null) {
                throw new FileNotFoundException("Gambar TTD tidak ditemukan");
            }

            // Parameter report
            HashMap<String, Object> params = new HashMap<>();
            params.put("TTD_IMAGE", ttdStream);
            
            // Generate report
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                reportPath,
                params,
                conn
            );
            
            // Export ke PDF
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
                "attachment; filename=\"Laporan_Pekerjaan_"+System.currentTimeMillis()+".pdf\"");
            
            JasperExportManager.exportReportToPdfStream(jasperPrint, response.getOutputStream());
            
        } catch (Exception e) {
            e.printStackTrace();
            request.getSession().setAttribute("error", "Gagal membuat laporan: " + e.getMessage());
            response.sendRedirect("LaporanPekerjaan");
        } finally {
            try { if(conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
}