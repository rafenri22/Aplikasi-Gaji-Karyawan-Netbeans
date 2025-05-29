/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unpam.view;

import com.unpam.model.Koneksi;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.util.JRLoader;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rafky
 */
@WebServlet(name = "LaporanGaji", urlPatterns = {"/LaporanGaji"})
public class LaporanGaji extends HttpServlet {

    private void tampilkanForm(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
    
        String formKonten = "<div class='card' style='max-width: 600px; margin: 2rem auto;'>" +
                            "<div class='card-header' style='padding: 1.5rem; border-bottom: 1px solid #eee;'>" +
                            "   <h2 style='color: var(--primary); margin: 0; text-align: center;'>" +
                            "      <i class='fas fa-file-invoice-dollar'></i> Cetak Laporan Gaji" +
                            "   </h2>" +
                            "</div>" +

                            "<div class='card-body' style='padding: 2rem;'>" +
                            "   <form method='GET' action='LaporanGaji'>" +
                            "      <div style='margin-bottom: 1.5rem;'>" +
                            "         <label style='display: block; margin-bottom: 0.5rem; color: #555; font-weight: 500;'>Nomor KTP</label>" +
                            "         <input type='text' name='ktp' required " +
                            "                class='form-input'" +
                            "                placeholder='Masukkan nomor KTP'>" +
                            "      </div>" +

                            "      <button type='submit' class='btn-primary'>" +
                            "         <i class='fas fa-print'></i> Cetak per Karyawan" +
                            "      </button>" +
                            "   </form>" +

                            "   <div class='separator' style='margin: 2rem 0; position: relative;'>" +
                            "      <div style='height: 1px; background-color: #eee;'></div>" +
                            "      <span style='position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); " +
                            "            background: white; padding: 0 1rem; color: #666; font-size: 0.9em;'>atau</span>" +
                            "   </div>" +

                            "   <a href='LaporanGaji?action=semua' class='btn-secondary'>" +
                            "      <i class='fas fa-users'></i> Cetak Semua Karyawan" +
                            "   </a>" +
                            "</div>" +
                            "</div>" +

                            "<style>" +
                            ".form-input {" +
                            "   width: 100%;" +
                            "   padding: 0.75rem;" +
                            "   border: 1px solid #ddd;" +
                            "   border-radius: 6px;" +
                            "   font-size: 1em;" +
                            "   transition: all 0.3s ease;" +
                            "}" +

                            ".form-input:focus {" +
                            "   border-color: var(--secondary);" +
                            "   box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);" +
                            "   outline: none;" +
                            "}" +

                            ".btn-primary {" +
                            "   width: 100%;" +
                            "   padding: 0.875rem;" +
                            "   background: linear-gradient(135deg, var(--primary), var(--secondary));" +
                            "   color: white;" +
                            "   border: none;" +
                            "   border-radius: 6px;" +
                            "   font-size: 1em;" +
                            "   cursor: pointer;" +
                            "   transition: all 0.3s ease;" +
                            "}" +

                            ".btn-primary:hover {" +
                            "   opacity: 0.9;" +
                            "   box-shadow: 0 4px 12px rgba(52, 152, 219, 0.2);" +
                            "}" +

                            ".btn-secondary {" +
                            "   display: block;" +
                            "   width: 100%;" +
                            "   padding: 0.875rem;" +
                            "   background: #f8f9fa;" +
                            "   color: var(--primary);" +
                            "   border: 1px solid #ddd;" +
                            "   border-radius: 6px;" +
                            "   text-align: center;" +
                            "   text-decoration: none;" +
                            "   font-size: 1em;" +
                            "   transition: all 0.3s ease;" +
                            "}" +

                            ".btn-secondary:hover {" +
                            "   background: var(--secondary);" +
                            "   color: white;" +
                            "   border-color: var(--secondary);" +
                            "}" +
                            "</style>";

        request.setAttribute("konten", formKonten);
        request.setAttribute("activeMenu", "laporan");

        RequestDispatcher dispatcher = request.getRequestDispatcher("/MainForm");
        dispatcher.forward(request, response);
    }

    private void generateReport(HttpServletResponse response, String reportName, Map<String, Object> params) 
            throws Exception {
        
        try (Connection conn = new Koneksi().getConnection()) {
            // 1. Load compiled report (.jasper)
            InputStream jasperStream = getServletContext().getResourceAsStream("/reports/" + reportName + ".jasper");
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);

            // 2. Tambahkan gambar tanda tangan ke parameter
            params.put("TTD_IMAGE", getServletContext().getResourceAsStream("/reports/images/ttd_ceo.png"));

            // 3. Generate report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, conn);

            // 4. Export ke PDF
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
            "attachment; filename=laporan_gaji_" + reportName + ".pdf");

            OutputStream out = response.getOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, out);
            out.flush();
        }
        
        // Ambil gambar sebagai InputStream
            InputStream ttdStream = getServletContext().getResourceAsStream("/reports/images/ttd_ceo.png");

            if (ttdStream == null) {
                throw new FileNotFoundException("Gambar TTD tidak ditemukan di path /reports/images/ttd_ceo.png");
            }

            // Parameter report
            params.put("TTD_IMAGE", ttdStream);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String ktp = request.getParameter("ktp");
        String action = request.getParameter("action");

        try {
            if ("semua".equals(action)) {
                generateReport(response, "gaji_semua", new HashMap<>());
            } else if (ktp != null && !ktp.isEmpty()) {
                Map<String, Object> params = new HashMap<>();
                params.put("KTP", ktp);
                generateReport(response, "gaji_seseorang", params);
            } else {
                tampilkanForm(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Gagal generate laporan: " + e.getMessage());
        }
    }
}