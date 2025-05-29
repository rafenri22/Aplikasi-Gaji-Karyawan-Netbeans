/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.unpam.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author rafky
 */
public class Koneksi {
    private Connection connection;
    private String pesanKesalahan;
    
    public Connection getConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/dbaplikasigajikaryawan", 
                "root", 
                "");
            return connection;
        } catch (Exception e) {
            pesanKesalahan = e.getMessage();
            return null;
        }
    }
    
    public String getPesanKesalahan() {
        return pesanKesalahan;
    }
}