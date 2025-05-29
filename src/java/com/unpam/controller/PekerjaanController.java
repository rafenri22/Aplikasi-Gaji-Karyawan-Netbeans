package com.unpam.controller;

import com.unpam.model.Pekerjaan;
import com.unpam.model.Koneksi;
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

/**
 *
 * @author rafky
 */
@WebServlet(name = "PekerjaanController", urlPatterns = {"/PekerjaanController"})
public class PekerjaanController extends HttpServlet {
    private final Pekerjaan pekerjaanModel = new Pekerjaan();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String formContent = generatePekerjaanForm(request);
        request.setAttribute("konten", formContent);
        // Set active menu
        request.setAttribute("activeMenu", "master");
        request.getRequestDispatcher("/MainForm").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        String action = request.getParameter("action");
        
        switch(action) {
            case "simpan":
                processSimpan(request, session);
                break;
            case "hapus":
                processHapus(request, session);
                break;
            case "cari":
                processCari(request, session);
                break;
            case "pilih":
                processPilih(request, session);
                break;
            case "lihat":
                processLihat(session);
                break;
        }
        response.sendRedirect("PekerjaanController");
    }

    private String generatePekerjaanForm(HttpServletRequest request) {
        HttpSession session = request.getSession();
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
           .append(".btn-danger { background: linear-gradient(135deg, #e74c3c, #c0392b); color: white; }")
           .append(".btn-danger:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(231, 76, 60, 0.3); }")
           .append(".btn-success { background: linear-gradient(135deg, #27ae60, #229954); color: white; }")
           .append(".btn-success:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(39, 174, 96, 0.3); }")
           .append(".btn-warning { background: linear-gradient(135deg, #f39c12, #e67e22); color: white; }")
           .append(".btn-warning:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(243, 156, 18, 0.3); }")
           .append(".data-table { width: 100%; border-collapse: collapse; margin: 1.5rem 0; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }")
           .append(".data-table thead { background: linear-gradient(135deg, var(--primary), #34495e); color: white; }")
           .append(".data-table th, .data-table td { padding: 1rem; text-align: left; border-bottom: 1px solid #eee; }")
           .append(".data-table tbody tr:hover { background: #f8f9fa; transform: scale(1.01); transition: all 0.3s; }")
           .append(".data-table tbody tr:nth-child(even) { background: #f8f9fa; }")
           .append(".form-group { margin-bottom: 1.5rem; }")
           .append(".form-group label { display: block; margin-bottom: 0.5rem; font-weight: 600; color: var(--primary); }")
           .append(".form-group input, .form-group select { width: 100%; padding: 0.75rem; border: 2px solid #e9ecef; border-radius: 6px; font-size: 1rem; transition: all 0.3s; }")
           .append(".form-group input:focus, .form-group select:focus { outline: none; border-color: var(--secondary); box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1); }")
           .append(".form-actions { display: flex; gap: 1rem; margin-top: 2rem; flex-wrap: wrap; }")
           .append(".page-header { background: linear-gradient(135deg, var(--primary), var(--secondary)); color: white; padding: 2rem; border-radius: 10px; margin-bottom: 2rem; text-align: center; }")
           .append(".page-header h1 { font-size: 2rem; margin-bottom: 0.5rem; }")
           .append(".page-header p { font-size: 1.1rem; opacity: 0.9; }")
           .append(".section-title { color: var(--primary); margin: 2rem 0 1rem 0; font-size: 1.5rem; font-weight: 600; display: flex; align-items: center; }")
           .append(".section-title i { margin-right: 0.5rem; color: var(--secondary); }")
           .append(".stats-card { background: linear-gradient(135deg, #f8f9fa, #e9ecef); padding: 1.5rem; border-radius: 10px; text-align: center; margin-bottom: 2rem; }")
           .append(".stats-number { font-size: 2rem; font-weight: bold; color: #8e44ad; }")
           .append(".action-buttons { display: flex; gap: 0.5rem; flex-wrap: wrap; }")
           .append(".task-badge { background: linear-gradient(135deg, #17a2b8, #138496); color: white; padding: 0.25rem 0.75rem; border-radius: 20px; font-size: 0.85rem; font-weight: 500; }")
           .append(".job-card { background: linear-gradient(135deg, #ffffff, #f8f9fa); border: 1px solid #e9ecef; border-radius: 10px; padding: 1.5rem; margin-bottom: 1rem; transition: all 0.3s; }")
           .append(".job-card:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(0,0,0,0.1); }")
           .append(".form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 2rem; }")
           .append("</style>");

        // Page Header
        html.append("<div class='page-header'>")
           .append("<h1><i class='fas fa-briefcase'></i> Manajemen Data Pekerjaan</h1>")
           .append("<p>Kelola jenis pekerjaan dan tugas dengan sistem yang terintegrasi</p>")
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
           .append("<h2 class='section-title'><i class='fas fa-cogs'></i> Aksi Cepat</h2>")
           .append("<div style='display: flex; gap: 1rem; flex-wrap: wrap;'>")
           .append("<form method='post' style='display: inline;'>")
           .append("<input type='hidden' name='action' value='lihat'>")
           .append("<button type='submit' class='btn btn-secondary'><i class='fas fa-list'></i> Lihat Semua Pekerjaan</button>")
           .append("</form>")
           .append("<button onclick='scrollToForm()' class='btn btn-primary'><i class='fas fa-plus'></i> Tambah Pekerjaan Baru</button>")
           .append("</div>")
           .append("</div>");

        // Tabel Data Pekerjaan
        if(session.getAttribute("pekerjaanList") != null) {
            Object[][] list = (Object[][]) session.getAttribute("pekerjaanList");
            
            html.append("<div class='card'>")
               .append("<h2 class='section-title'><i class='fas fa-table'></i> Daftar Pekerjaan</h2>")
               .append("<div class='stats-card'>")
               .append("<div class='stats-number'>").append(list.length).append("</div>")
               .append("<p>Total Jenis Pekerjaan Tersedia</p>")
               .append("</div>")
               .append("<div style='overflow-x: auto;'>")
               .append("<table class='data-table'>")
               .append("<thead>")
               .append("<tr>")
               .append("<th><i class='fas fa-code'></i> Kode Pekerjaan</th>")
               .append("<th><i class='fas fa-briefcase'></i> Nama Pekerjaan</th>")
               .append("<th><i class='fas fa-tasks'></i> Jumlah Tugas</th>")
               .append("<th><i class='fas fa-tools'></i> Aksi</th>")
               .append("</tr>")
               .append("</thead>")
               .append("<tbody>");
            
            for(Object[] row : list) {
                html.append("<tr>")
                   .append("<td><strong style='color: #8e44ad;'>").append(row[0]).append("</strong></td>")
                   .append("<td>").append(row[1]).append("</td>")
                   .append("<td><span class='task-badge'>").append(row[2]).append(" Tugas</span></td>")
                   .append("<td>")
                   .append("<div class='action-buttons'>")
                   .append("<form method='post' style='display:inline;'>")
                   .append("<input type='hidden' name='action' value='pilih'>")
                   .append("<input type='hidden' name='kode' value='").append(row[0]).append("'>")
                   .append("<button type='submit' class='btn btn-success' style='padding: 0.5rem 1rem; font-size: 0.9rem;'><i class='fas fa-edit'></i> Edit</button>")
                   .append("</form>")
                   .append("<form method='post' style='display:inline;' onsubmit='return confirm(\"Yakin ingin menghapus pekerjaan ini?\")'>")
                   .append("<input type='hidden' name='action' value='hapus'>")
                   .append("<input type='hidden' name='kode' value='").append(row[0]).append("'>")
                   .append("<button type='submit' class='btn btn-danger' style='padding: 0.5rem 1rem; font-size: 0.9rem;'><i class='fas fa-trash'></i> Hapus</button>")
                   .append("</form>")
                   .append("</div>")
                   .append("</td>")
                   .append("</tr>");
            }
            html.append("</tbody></table>")
               .append("</div>")
               .append("</div>");
            session.removeAttribute("pekerjaanList");
        }

        // Form Input
        boolean isUpdate = session.getAttribute("kode") != null;
        html.append("<div class='card' id='formSection'>")
           .append("<h2 class='section-title'>")
           .append("<i class='fas fa-").append(isUpdate ? "edit" : "plus-circle").append("'></i> ")
           .append(isUpdate ? "Update Data Pekerjaan" : "Tambah Pekerjaan Baru")
           .append("</h2>")
           .append("<form method='post'>")
           .append("<input type='hidden' name='action' value='simpan'>")
           
           .append("<div class='form-grid'>")
           
           // Kolom Kiri
           .append("<div>")
           .append("<div class='form-group'>")
           .append("<label><i class='fas fa-code'></i> Kode Pekerjaan</label>")
           .append("<input type='text' name='kode' value='").append(session.getAttribute("kode") != null ? session.getAttribute("kode") : "").append("' required placeholder='Contoh: RFA001, RFA002'>")
           .append("<small style='color: #6c757d; font-size: 0.875rem;'>Gunakan kode unik untuk setiap jenis pekerjaan</small>")
           .append("</div>")
           .append("<div class='form-group'>")
           .append("<label><i class='fas fa-briefcase'></i> Nama Pekerjaan</label>")
           .append("<input type='text' name='nama' value='").append(session.getAttribute("nama") != null ? session.getAttribute("nama") : "").append("' required placeholder='Contoh: Developer, Marketing'>")
           .append("<small style='color: #6c757d; font-size: 0.875rem;'>Masukkan nama pekerjaan yang jelas</small>")
           .append("</div>")
           .append("</div>")
           
           // Kolom Kanan
           .append("<div>")
           .append("<div class='form-group'>")
           .append("<label><i class='fas fa-tasks'></i> Jumlah Tugas</label>")
           .append("<select name='tugas'>");
        for(int i=1; i<=10; i++) {
            html.append("<option value='").append(i).append("'")
               .append(i == (session.getAttribute("tugas") != null ? 
                       Integer.parseInt(session.getAttribute("tugas").toString()) : 1) ? " selected" : "")
               .append(">").append(i).append(" Tugas</option>");
        }
        html.append("</select>")
           .append("<small style='color: #6c757d; font-size: 0.875rem;'>Tentukan jumlah tugas yang akan dikerjakan</small>")
           .append("</div>")
           
           // Info Card
           .append("<div class='job-card'>")
           .append("<h4 style='color: #8e44ad; margin-bottom: 1rem;'><i class='fas fa-info-circle'></i> Informasi</h4>")
           .append("<ul style='margin: 0; padding-left: 1.5rem; color: #6c757d;'>")
           .append("<li>Kode pekerjaan harus unik</li>")
           .append("<li>Nama pekerjaan akan tampil di form karyawan</li>")
           .append("</ul>")
           .append("</div>")
           .append("</div>")
           .append("</div>")
           
           .append("<div class='form-actions'>")
           .append("<button type='submit' class='btn ").append(isUpdate ? "btn-success" : "btn-primary").append("'>")
           .append("<i class='fas fa-").append(isUpdate ? "save" : "plus").append("'></i> ")
           .append(isUpdate ? "Update Data" : "Simpan Data")
           .append("</button>");
        
        if(isUpdate) {
            html.append("<button type='button' onclick='location.href=\"PekerjaanController\"' class='btn btn-secondary'>")
               .append("<i class='fas fa-times'></i> Batal")
               .append("</button>");
        }
        
        html.append("<button type='button' onclick='resetForm()' class='btn btn-warning'>")
           .append("<i class='fas fa-undo'></i> Reset Form")
           .append("</button>")
           .append("</div>")
           .append("</form>")
           .append("</div>");

        // JavaScript untuk interaktivitas
        html.append("<script>")
           .append("function scrollToForm() {")
           .append("  document.getElementById('formSection').scrollIntoView({ behavior: 'smooth' });")
           .append("}")
           .append("function resetForm() {")
           .append("  if(confirm('Yakin ingin mereset form?')) {")
           .append("    document.querySelector('form input[name=\"kode\"]').value = '';")
           .append("    document.querySelector('form input[name=\"nama\"]').value = '';")
           .append("    document.querySelector('form select[name=\"tugas\"]').selectedIndex = 0;")
           .append("  }")
           .append("}")
           .append("</script>");

        session.removeAttribute("kode");
        session.removeAttribute("nama");
        session.removeAttribute("tugas");
        return html.toString();
    }

    private void processSimpan(HttpServletRequest request, HttpSession session) {
        try {
            pekerjaanModel.setKodePekerjaan(request.getParameter("kode"));
            pekerjaanModel.setNamaPekerjaan(request.getParameter("nama"));
            pekerjaanModel.setJumlahTugas(Integer.parseInt(request.getParameter("tugas")));
            
            if(pekerjaanModel.simpan()) {
                session.setAttribute("message", "Data pekerjaan berhasil disimpan");
            } else {
                session.setAttribute("error", "Gagal menyimpan: " + pekerjaanModel.getPesan());
            }
        } catch (Exception e) {
            session.setAttribute("error", "Error: " + e.getMessage());
        }
    }

    private void processHapus(HttpServletRequest request, HttpSession session) {
        String kode = request.getParameter("kode");
        if(kode != null && !kode.isEmpty()) {
            if(pekerjaanModel.hapus(kode)) {
                session.setAttribute("message", "Data pekerjaan berhasil dihapus");
                processLihat(session);
            } else {
                session.setAttribute("error", "Gagal menghapus: " + pekerjaanModel.getPesan());
            }
        }
    }

    private void processCari(HttpServletRequest request, HttpSession session) {
        String kode = request.getParameter("cariKode");
        if(kode != null && !kode.isEmpty()) {
            if(pekerjaanModel.baca(kode)) {
                session.setAttribute("kode", pekerjaanModel.getKodePekerjaan());
                session.setAttribute("nama", pekerjaanModel.getNamaPekerjaan());
                session.setAttribute("tugas", pekerjaanModel.getJumlahTugas());
            } else {
                session.setAttribute("error", pekerjaanModel.getPesan());
            }
        }
    }

    private void processLihat(HttpSession session) {
        if(pekerjaanModel.bacaData()) {
            session.setAttribute("pekerjaanList", pekerjaanModel.getList());
        } else {
            session.setAttribute("error", "Gagal memuat data: " + pekerjaanModel.getPesan());
        }
    }

    private void processPilih(HttpServletRequest request, HttpSession session) {
        String kode = request.getParameter("kode");
        if(kode != null && !kode.isEmpty() && pekerjaanModel.baca(kode)) {
            session.setAttribute("kode", pekerjaanModel.getKodePekerjaan());
            session.setAttribute("nama", pekerjaanModel.getNamaPekerjaan());
            session.setAttribute("tugas", pekerjaanModel.getJumlahTugas());
        }
    }
}