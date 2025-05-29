/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unpam.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.unpam.view.PesanDialog;
import java.sql.Statement;

/**
 *
 * @author rafky
 */

public class Karyawan {
    private String ktp;
    private String nama;
    private int ruang;
    private String password;
    private String kodePekerjaan;
    private String pesan;
    private Object[][] list;
    private final Koneksi koneksi = new Koneksi();

    // Getter dan Setter
    public String getKtp() { return ktp; }
    public void setKtp(String ktp) { this.ktp = ktp; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public int getRuang() { return ruang; }
    public void setRuang(int ruang) { this.ruang = ruang; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getKodePekerjaan() { return kodePekerjaan; }
    public void setKodePekerjaan(String kodePekerjaan) { this.kodePekerjaan = kodePekerjaan; }
    public String getPesan() { return pesan; }
    public Object[][] getList() { return list; }

    public boolean simpan() {
        boolean sukses = false;
        try (Connection conn = koneksi.getConnection()) {
            String sql = "INSERT INTO tbkaryawan (ktp, nama, ruang, password, kode_pekerjaan) "
                       + "VALUES (?, ?, ?, ?, ?) "
                       + "ON DUPLICATE KEY UPDATE nama=?, ruang=?, password=?, kode_pekerjaan=?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ktp);
                stmt.setString(2, nama);
                stmt.setInt(3, ruang);
                stmt.setString(4, password);
                stmt.setString(5, kodePekerjaan);
                stmt.setString(6, nama);
                stmt.setInt(7, ruang);
                stmt.setString(8, password);
                stmt.setString(9, kodePekerjaan);

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    sukses = true;
                }
            }
        } catch (SQLException ex) {
            pesan = "Error database: " + ex.getMessage();
        }
        return sukses;
    }

    public boolean baca(String ktp) {
        boolean found = false;
        try (Connection conn = koneksi.getConnection()) {
            String sql = "SELECT * FROM tbkaryawan WHERE ktp = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ktp);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    this.ktp = rs.getString("ktp");
                    nama = rs.getString("nama");
                    ruang = rs.getInt("ruang");
                    password = rs.getString("password");
                    kodePekerjaan = rs.getString("kode_pekerjaan");
                    found = true;
                }
            }
        } catch (SQLException ex) {
            pesan = "Error database: " + ex.getMessage();
        }
        return found;
    }

    public boolean bacaData() {
        boolean sukses = false;
        try (Connection conn = koneksi.getConnection()) {
            String sql = "SELECT ktp, nama FROM tbkaryawan";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                rs.last();
                list = new Object[rs.getRow()][2];
                rs.beforeFirst();
                
                int i = 0;
                while (rs.next()) {
                    list[i][0] = rs.getString("ktp");
                    list[i][1] = rs.getString("nama");
                    i++;
                }
                sukses = true;
            }
        } catch (SQLException ex) {
            pesan = "Error database: " + ex.getMessage();
        }
        return sukses;
    }

    public boolean hapus(String ktp) {
        boolean sukses = false;
        try (Connection conn = koneksi.getConnection()) {
            String sql = "DELETE FROM tbkaryawan WHERE ktp = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ktp);
                int affectedRows = stmt.executeUpdate();
                sukses = affectedRows > 0;
            }
        } catch (SQLException ex) {
            pesan = "Error database: " + ex.getMessage();
        }
        return sukses;
    }
    
    public boolean simpanGajiAwal(String ktp, String kodePekerjaan) {
        boolean sukses = false;
        try (Connection conn = koneksi.getConnection()) {
            String sql = "INSERT INTO tbgaji (ktp, kodepekerjaan, gajibersih, gajikotor, tunjangan) "
                       + "VALUES (?, ?, 0, 0, 0)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ktp);
                stmt.setString(2, kodePekerjaan);
                int affectedRows = stmt.executeUpdate();
                sukses = affectedRows > 0;
            }
        } catch (SQLException ex) {
            pesan = "Error database: " + ex.getMessage();
        }
        return sukses;
    }
}