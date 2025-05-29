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
import java.sql.Statement; 
import java.util.HashMap;  

/**
 *
 * @author rafky
 */

public class Gaji {
    private String ktp; 
    private String pesan; 
    private Object[][] listGaji; 
    public final Koneksi koneksi = new Koneksi(); 
 
    public String getKtp() { 
        return ktp; 
    } 
 
    public void setKtp(String ktp) { 
        this.ktp = ktp; 
    } 
 
    public String getPesan() { 
        return pesan; 
    } 
 
    public void setPesan(String pesan) { 
        this.pesan = pesan; 
    }
    
    public Object[][] getListGaji() { 
        return listGaji; 
    } 
 
    public void setListGaji(Object[][] listGaji) { 
        this.listGaji = listGaji; 
    } 
     
    // Di method simpan()
    public boolean simpan() {
        boolean adaKesalahan = false;
        Connection connection;

        if ((connection = koneksi.getConnection()) != null) {
            try {
                // Hapus data gaji lama
                String deleteSQL = "DELETE FROM tbgaji WHERE ktp=?";
                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSQL)) {
                    deleteStmt.setString(1, ktp);
                    deleteStmt.executeUpdate();
                }

                // Simpan data gaji baru
                String insertSQL = "INSERT INTO tbgaji (ktp, kodepekerjaan, gajibersih, gajikotor, tunjangan) "
                                 + "VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                    for (Object[] recGaji : listGaji) {
                        if (recGaji[0] != null && !recGaji[0].toString().isEmpty()) {
                            insertStmt.setString(1, ktp);
                            for (int i = 0; i < 4; i++) {
                                insertStmt.setString(2 + i, recGaji[i].toString());
                            }
                            insertStmt.executeUpdate();
                        }
                    }
                }

                connection.close();
            } catch (SQLException ex) {
                adaKesalahan = true;
                pesan = "Gagal menyimpan data gaji: " + ex.getMessage();
            }
        } else {
            adaKesalahan = true;
            pesan = "Gagal koneksi ke database: " + koneksi.getPesanKesalahan();
        }

        return !adaKesalahan;
    } 
     
    public boolean baca(String ktp){ 
        boolean adaKesalahan = false;  
        Connection connection;  
        this.ktp = ktp; 
        listGaji = null; 
        if ((connection = koneksi.getConnection()) != null){
            String SQLStatemen; 
            PreparedStatement preparedStatement; 
            ResultSet rset; 
             
            try { 
                SQLStatemen = "select * from tbgaji where ktp=?";  
                preparedStatement = connection.prepareStatement(SQLStatemen); 
                preparedStatement.setString(1, ktp); 
                rset = preparedStatement.executeQuery(); 
                 
                rset.next(); 
                rset.last(); 
                listGaji = new Object[rset.getRow()][4]; 
                 
                rset.first(); 
                int i=0; 
                do {  
                    if (!rset.getString("kodepekerjaan").equals("")){
                        listGaji[i] = new Object[]{ rset.getString("kodepekerjaan"),  
                            rset.getObject("gajibersih"), rset.getObject("gajikotor"), 
                            rset.getObject("tunjangan")};        
                    } 
                    i++; 
                } while (rset.next()); 
                 
                if (listGaji.length > 0) { 
                    adaKesalahan = false; 
                } 
                preparedStatement.close(); 
                rset.close(); 
                connection.close(); 
            } catch (SQLException ex){ 
                adaKesalahan = true; 
                pesan = "Tidak dapat membaca data gaji karyawan\n"+ex.getMessage(); 
            } 
        } else { 
            adaKesalahan = true;
            pesan = "Tidak dapat melakukan koneksi ke server\n"+koneksi.getPesanKesalahan(); 
        } 
        return !adaKesalahan; 
    }     
}
