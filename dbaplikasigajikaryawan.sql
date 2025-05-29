-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 28, 2025 at 07:31 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `dbaplikasigajikaryawan`
--

-- --------------------------------------------------------

--
-- Table structure for table `tbgaji`
--

CREATE TABLE `tbgaji` (
  `id` int(11) NOT NULL,
  `ktp` varchar(20) NOT NULL,
  `kodepekerjaan` varchar(10) NOT NULL,
  `gajibersih` decimal(12,2) DEFAULT 0.00,
  `gajikotor` decimal(12,2) DEFAULT 0.00,
  `tunjangan` decimal(12,2) DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tbgaji`
--

INSERT INTO `tbgaji` (`id`, `ktp`, `kodepekerjaan`, `gajibersih`, `gajikotor`, `tunjangan`) VALUES
(20, '221011401796', 'RFA001', 5000000.00, 500000.00, 250000.00),
(21, '221011401797', 'RFA001', 5000000.00, 500000.00, 250000.00),
(22, '221011401798', 'RFA001', 5000000.00, 500000.00, 250000.00),
(23, '221011401799', 'RFA002', 5000000.00, 250000.00, 600000.00),
(24, '221011401795', 'RFA003', 5000000.00, 500000.00, 100000.00),
(25, '221011401791', 'RFA004', 6000000.00, 100000.00, 250000.00),
(27, '221011401777', 'RFA004', 10000.00, 10000.00, 0.00);

-- --------------------------------------------------------

--
-- Table structure for table `tbkaryawan`
--

CREATE TABLE `tbkaryawan` (
  `ktp` varchar(20) NOT NULL,
  `nama` varchar(50) NOT NULL,
  `ruang` int(11) NOT NULL,
  `password` varchar(100) NOT NULL,
  `kode_pekerjaan` varchar(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tbkaryawan`
--

INSERT INTO `tbkaryawan` (`ktp`, `nama`, `ruang`, `password`, `kode_pekerjaan`) VALUES
('221011401777', 'Mbappe', 3, '9720f047b7055320d6df8fc33972bc2d', 'RFA004'),
('221011401791', 'Messi', 5, '9720f047b7055320d6df8fc33972bc2d', 'RFA004'),
('221011401795', 'Ronaldo', 3, '9720f047b7055320d6df8fc33972bc2d', 'RFA003'),
('221011401796', 'Rafky', 1, '9720f047b7055320d6df8fc33972bc2d', 'RFA001'),
('221011401797', 'Ferdian', 1, '9720f047b7055320d6df8fc33972bc2d', 'RFA001'),
('221011401798', 'Algiffari', 1, '9720f047b7055320d6df8fc33972bc2d', 'RFA001'),
('221011401799', 'Cristiano', 2, '9720f047b7055320d6df8fc33972bc2d', 'RFA002');

-- --------------------------------------------------------

--
-- Table structure for table `tbpekerjaan`
--

CREATE TABLE `tbpekerjaan` (
  `kodepekerjaan` varchar(10) NOT NULL,
  `namapekerjaan` varchar(50) NOT NULL,
  `jumlahtugas` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tbpekerjaan`
--

INSERT INTO `tbpekerjaan` (`kodepekerjaan`, `namapekerjaan`, `jumlahtugas`) VALUES
('RFA001', 'Developer', 5),
('RFA002', 'Marketing', 1),
('RFA003', 'IT Support', 6),
('RFA004', 'Admin', 7),
('RFA005', 'Sales', 3);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `tbgaji`
--
ALTER TABLE `tbgaji`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_gaji_karyawan` (`ktp`),
  ADD KEY `fk_gaji_pekerjaan` (`kodepekerjaan`);

--
-- Indexes for table `tbkaryawan`
--
ALTER TABLE `tbkaryawan`
  ADD PRIMARY KEY (`ktp`),
  ADD KEY `bkaryawan_ibfk_t` (`kode_pekerjaan`);

--
-- Indexes for table `tbpekerjaan`
--
ALTER TABLE `tbpekerjaan`
  ADD PRIMARY KEY (`kodepekerjaan`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `tbgaji`
--
ALTER TABLE `tbgaji`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=28;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `tbgaji`
--
ALTER TABLE `tbgaji`
  ADD CONSTRAINT `fk_gaji_karyawan` FOREIGN KEY (`ktp`) REFERENCES `tbkaryawan` (`ktp`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_gaji_pekerjaan` FOREIGN KEY (`kodepekerjaan`) REFERENCES `tbpekerjaan` (`kodepekerjaan`) ON DELETE CASCADE;

--
-- Constraints for table `tbkaryawan`
--
ALTER TABLE `tbkaryawan`
  ADD CONSTRAINT `bkaryawan_ibfk_t` FOREIGN KEY (`kode_pekerjaan`) REFERENCES `tbpekerjaan` (`kodepekerjaan`) ON DELETE CASCADE,
  ADD CONSTRAINT `tbkaryawan_ibfk_1` FOREIGN KEY (`kode_pekerjaan`) REFERENCES `tbpekerjaan` (`kodepekerjaan`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
