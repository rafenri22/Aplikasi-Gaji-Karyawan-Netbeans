package com.unpam.controller;

import com.unpam.model.Enkripsi;
import com.unpam.model.Karyawan;
import com.unpam.model.Pekerjaan;
import com.unpam.view.MainForm;
import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 *
 * @author rafky
 */
@WebServlet(name = "KaryawanController", urlPatterns = {"/KaryawanController"})
public class KaryawanController extends HttpServlet {
    private final Karyawan karyawanModel = new Karyawan();
    private final Enkripsi enkripsi = new Enkripsi();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String formContent = generateKaryawanForm(request);
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
        response.sendRedirect("KaryawanController");
    }

    private String generateKaryawanForm(HttpServletRequest request) {
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
           .append(".data-table { width: 100%; border-collapse: collapse; margin: 1.5rem 0; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }")
           .append(".data-table thead { background: linear-gradient(135deg, var(--primary), #34495e); color: white; }")
           .append(".data-table th, .data-table td { padding: 1rem; text-align: left; border-bottom: 1px solid #eee; }")
           .append(".data-table tbody tr:hover { background: #f8f9fa; transform: scale(1.01); transition: all 0.3s; }")
           .append(".data-table tbody tr:nth-child(even) { background: #f8f9fa; }")
           .append(".form-group { margin-bottom: 1.5rem; }")
           .append(".form-group label { display: block; margin-bottom: 0.5rem; font-weight: 600; color: var(--primary); }")
           .append(".form-group input, .form-group select { width: 100%; padding: 0.75rem; border: 2px solid #e9ecef; border-radius: 6px; font-size: 1rem; transition: all 0.3s; }")
           .append(".form-group input:focus, .form-group select:focus { outline: none; border-color: var(--secondary); box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1); }")
           .append(".form-actions { display: flex; gap: 1rem; margin-top: 2rem; }")
           .append(".page-header { background: linear-gradient(135deg, var(--primary), var(--secondary)); color: white; padding: 2rem; border-radius: 10px; margin-bottom: 2rem; text-align: center; }")
           .append(".page-header h1 { font-size: 2rem; margin-bottom: 0.5rem; }")
           .append(".page-header p { font-size: 1.1rem; opacity: 0.9; }")
           .append(".section-title { color: var(--primary); margin: 2rem 0 1rem 0; font-size: 1.5rem; font-weight: 600; display: flex; align-items: center; }")
           .append(".section-title i { margin-right: 0.5rem; color: var(--secondary); }")
           .append(".stats-card { background: linear-gradient(135deg, #f8f9fa, #e9ecef); padding: 1.5rem; border-radius: 10px; text-align: center; margin-bottom: 2rem; }")
           .append(".stats-number { font-size: 2rem; font-weight: bold; color: var(--secondary); }")
           .append(".action-buttons { display: flex; gap: 0.5rem; }")
           .append("</style>");

        // Page Header
        html.append("<div class='page-header'>")
           .append("<h1><i class='fas fa-users'></i> Manajemen Data Karyawan</h1>")
           .append("<p>Kelola informasi karyawan dengan mudah dan efisien</p>")
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
           .append("<button type='submit' class='btn btn-secondary'><i class='fas fa-list'></i> Lihat Semua Karyawan</button>")
           .append("</form>")
           .append("<button onclick='scrollToForm()' class='btn btn-primary'><i class='fas fa-plus'></i> Tambah Karyawan Baru</button>")
           .append("</div>")
           .append("</div>");

        // Tabel Data Karyawan
        if(session.getAttribute("karyawanList") != null) {
            Object[][] list = (Object[][]) session.getAttribute("karyawanList");
            
            html.append("<div class='card'>")
               .append("<h2 class='section-title'><i class='fas fa-table'></i> Daftar Karyawan</h2>")
               .append("<div class='stats-card'>")
               .append("<div class='stats-number'>").append(list.length).append("</div>")
               .append("<p>Total Karyawan Terdaftar</p>")
               .append("</div>")
               .append("<div style='overflow-x: auto;'>")
               .append("<table class='data-table'>")
               .append("<thead>")
               .append("<tr>")
               .append("<th><i class='fas fa-id-card'></i> No. KTP</th>")
               .append("<th><i class='fas fa-user'></i> Nama Lengkap</th>")
               .append("<th><i class='fas fa-tools'></i> Aksi</th>")
               .append("</tr>")
               .append("</thead>")
               .append("<tbody>");
            
            for(Object[] row : list) {
                html.append("<tr>")
                   .append("<td><strong>").append(row[0]).append("</strong></td>")
                   .append("<td>").append(row[1]).append("</td>")
                   .append("<td>")
                   .append("<div class='action-buttons'>")
                   .append("<form method='post' style='display:inline;'>")
                   .append("<input type='hidden' name='action' value='pilih'>")
                   .append("<input type='hidden' name='ktp' value='").append(row[0]).append("'>")
                   .append("<button type='submit' class='btn btn-success' style='padding: 0.5rem 1rem; font-size: 0.9rem;'><i class='fas fa-edit'></i> Edit</button>")
                   .append("</form>")
                   .append("<form method='post' style='display:inline;' onsubmit='return confirm(\"Yakin ingin menghapus data ini?\")'>")
                   .append("<input type='hidden' name='action' value='hapus'>")
                   .append("<input type='hidden' name='ktp' value='").append(row[0]).append("'>")
                   .append("<button type='submit' class='btn btn-danger' style='padding: 0.5rem 1rem; font-size: 0.9rem;'><i class='fas fa-trash'></i> Hapus</button>")
                   .append("</form>")
                   .append("</div>")
                   .append("</td>")
                   .append("</tr>");
            }
            html.append("</tbody></table>")
               .append("</div>")
               .append("</div>");
            session.removeAttribute("karyawanList");
        }

        // Form Input
        boolean isUpdate = session.getAttribute("ktp") != null;
        html.append("<div class='card' id='formSection'>")
           .append("<h2 class='section-title'>")
           .append("<i class='fas fa-").append(isUpdate ? "edit" : "user-plus").append("'></i> ")
           .append(isUpdate ? "Update Data Karyawan" : "Tambah Karyawan Baru")
           .append("</h2>")
           .append("<form method='post'>")
           .append("<input type='hidden' name='action' value='simpan'>")
           
           .append("<div style='display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 2rem;'>")
           
           // Kolom Kiri
           .append("<div>")
           .append("<div class='form-group'>")
           .append("<label><i class='fas fa-id-card'></i> Nomor KTP</label>")
           .append("<input type='text' name='ktp' value='").append(session.getAttribute("ktp") != null ? session.getAttribute("ktp") : "").append("' required placeholder='Masukkan nomor KTP'>")
           .append("</div>")
           .append("<div class='form-group'>")
           .append("<label><i class='fas fa-user'></i> Nama Lengkap</label>")
           .append("<input type='text' name='nama' value='").append(session.getAttribute("nama") != null ? session.getAttribute("nama") : "").append("' required placeholder='Masukkan nama lengkap'>")
           .append("</div>")
           .append("<div class='form-group'>")
           .append("<label><i class='fas fa-door-open'></i> Ruang Kerja</label>")
           .append("<select name='ruang'>");
        for(int i=1; i<=5; i++) {
            html.append("<option value='").append(i).append("'")
               .append(i == (session.getAttribute("ruang") != null ? 
                       Integer.parseInt(session.getAttribute("ruang").toString()) : 1) ? " selected" : "")
               .append(">Ruang ").append(i).append("</option>");
        }
        html.append("</select>")
           .append("</div>")
           .append("</div>")
           
           // Kolom Kanan
           .append("<div>")
           .append("<div class='form-group'>")
           .append("<label><i class='fas fa-briefcase'></i> Jenis Pekerjaan</label>")
           .append("<select name='kode_pekerjaan'>");
        
        // Ambil data pekerjaan dari database
        Pekerjaan pekerjaanModel = new Pekerjaan();
        if(pekerjaanModel.bacaData()) {
            for(Object[] row : pekerjaanModel.getList()) {
                String kode = (String) row[0];
                String namaPekerjaan = (String) row[1];
                html.append("<option value='").append(kode).append("'")
                   .append(kode.equals(session.getAttribute("kode_pekerjaan")) ? " selected" : "")
                   .append(">").append(kode).append(" - ").append(namaPekerjaan).append("</option>");
            }
        }
        html.append("</select>")
           .append("</div>")
           .append("<div class='form-group'>")
           .append("<label><i class='fas fa-lock'></i> Password</label>")
           .append("<input type='password' name='password' required placeholder='Masukkan password'>")
           .append("</div>")
           .append("</div>")
           .append("</div>")
           
           .append("<div class='form-actions'>")
           .append("<button type='submit' class='btn ").append(isUpdate ? "btn-success" : "btn-primary").append("'>")
           .append("<i class='fas fa-").append(isUpdate ? "save" : "plus").append("'></i> ")
           .append(isUpdate ? "Update Data" : "Simpan Data")
           .append("</button>");
        
        if(isUpdate) {
            html.append("<button type='button' onclick='location.href=\"KaryawanController\"' class='btn btn-secondary'>")
               .append("<i class='fas fa-times'></i> Batal")
               .append("</button>");
        }
        html.append("</div>")
           .append("</form>")
           .append("</div>");

        // JavaScript untuk smooth scroll
        html.append("<script>")
           .append("function scrollToForm() {")
           .append("  document.getElementById('formSection').scrollIntoView({ behavior: 'smooth' });")
           .append("}")
           .append("</script>");

        session.removeAttribute("ktp");
        session.removeAttribute("nama");
        session.removeAttribute("ruang");
        session.removeAttribute("kode_pekerjaan");
        return html.toString();
    }

    private void processSimpan(HttpServletRequest request, HttpSession session) {
        try {
            String ktp = request.getParameter("ktp");
            String kodePekerjaan = request.getParameter("kode_pekerjaan");
            
            karyawanModel.setKtp(ktp);
            karyawanModel.setNama(request.getParameter("nama"));
            karyawanModel.setRuang(Integer.parseInt(request.getParameter("ruang")));
            karyawanModel.setKodePekerjaan(kodePekerjaan);
            
            String hashedPassword = enkripsi.hashMD5(request.getParameter("password"));
            karyawanModel.setPassword(hashedPassword);
            
            if(karyawanModel.simpan()) {
                // Simpan ke tabel gaji
                if(karyawanModel.simpanGajiAwal(ktp, kodePekerjaan)) {
                    session.setAttribute("message", "Data karyawan dan gaji awal berhasil disimpan");
                } else {
                    session.setAttribute("message", "Data karyawan disimpan, tetapi gagal membuat data gaji: " + karyawanModel.getPesan());
                }
            } else {
                session.setAttribute("error", "Gagal menyimpan: " + karyawanModel.getPesan());
            }
        } catch (Exception e) {
            session.setAttribute("error", "Error: " + e.getMessage());
        }
    }

    private void processHapus(HttpServletRequest request, HttpSession session) {
        String ktp = request.getParameter("ktp");
        if(ktp != null && !ktp.isEmpty()) {
            if(karyawanModel.hapus(ktp)) {
                session.setAttribute("message", "Data berhasil dihapus");
                processLihat(session);
            } else {
                session.setAttribute("error", "Gagal menghapus: " + karyawanModel.getPesan());
            }
        }
    }

    private void processCari(HttpServletRequest request, HttpSession session) {
        String ktp = request.getParameter("cariKtp");
        if(ktp != null && !ktp.isEmpty()) {
            if(karyawanModel.baca(ktp)) {
                session.setAttribute("ktp", karyawanModel.getKtp());
                session.setAttribute("nama", karyawanModel.getNama());
                session.setAttribute("ruang", karyawanModel.getRuang());
                session.setAttribute("kode_pekerjaan", karyawanModel.getKodePekerjaan());
            } else {
                session.setAttribute("error", karyawanModel.getPesan());
            }
        }
    }

    private void processLihat(HttpSession session) {
        if(karyawanModel.bacaData()) {
            session.setAttribute("karyawanList", karyawanModel.getList());
        } else {
            session.setAttribute("error", "Gagal memuat data: " + karyawanModel.getPesan());
        }
    }

    private void processPilih(HttpServletRequest request, HttpSession session) {
        String ktp = request.getParameter("ktp");
        if(ktp != null && !ktp.isEmpty() && karyawanModel.baca(ktp)) {
            session.setAttribute("ktp", karyawanModel.getKtp());
            session.setAttribute("nama", karyawanModel.getNama());
            session.setAttribute("ruang", karyawanModel.getRuang());
            session.setAttribute("kode_pekerjaan", karyawanModel.getKodePekerjaan());
        }
    }
}