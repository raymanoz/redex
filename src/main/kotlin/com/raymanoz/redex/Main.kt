package com.raymanoz.redex

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT
import org.apache.poi.ss.usermodel.IndexedColors.PALE_BLUE
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 2) usage()
    val configFile = args[0]
    val outputDir = args[1]

    val configuration = jacksonObjectMapper().readValue<Database>(File(configFile))

    SXSSFWorkbook().use { workbook ->
        val cellStyles = CellStyles(workbook)

        DriverManager.getConnection(configuration.jdbcUrl).use { connection ->
            println("Exporting data")
            configuration.tables.forEach { table ->
                export(connection, table, workbook, cellStyles)
            }
        }

        val outputFile = "${outputDir}/${configuration.name}.xlsx"
        println("writing $outputFile")
        FileOutputStream(outputFile).use {
            workbook.write(it)
        }
    }
}

private fun usage() {
    println("usage: redex config_file output_dir")
    println("  config_file: a json file containing details of what to export & redact.")
    println("  output_dir : the directory to output the xlsx file to. This will be named from the name property in the config file.")
    exitProcess(1)
}

private fun export(connection: Connection, table: Table, workbook: Workbook, cellStyles: CellStyles) {
    connection.prepareStatement("select * from ${table.name}").use { statement ->
        print("${table.name} ")
        val sheet = workbook.createSheet(table.name)

        statement.executeQuery().use { resultSet ->
            val columnCount = resultSet.metaData.columnCount
            createHeaderRow(columnCount, sheet, resultSet, cellStyles)

            while (resultSet.next()) {
                createValueRow(sheet, resultSet, columnCount, table, cellStyles)
                if (resultSet.row % 1000 == 0) print(".")
            }
            println()
        }
    }
}

private fun createValueRow(sheet: Sheet, resultSet: ResultSet, columnCount: Int, table: Table, cellStyles: CellStyles) {
    val row = sheet.createRow(resultSet.row)
    for (columnIndex in 1..columnCount) {
        val cell = row.createCell(columnIndex - 1)

        val columnName = resultSet.metaData.getColumnName(columnIndex)
        val value = if (columnName.isIn(table.redact)) {
            cell.cellStyle = cellStyles.redacted
            "** Redacted **"
        } else resultSet.getString(columnIndex)
        cell.setCellValue(value)
    }
}

private fun createHeaderRow(columnCount: Int, sheet: Sheet, resultSet: ResultSet, cellStyles: CellStyles) {
    val row = sheet.createRow(0)
    for (columnIndex in 0 until columnCount) {
        val cell = row.createCell(columnIndex)
        cell.setCellValue(resultSet.metaData.getColumnName(columnIndex + 1))
        cell.cellStyle = cellStyles.header
    }
}

private class CellStyles(workbook: Workbook,
                 val header: CellStyle = workbook.createCellStyle(),
                 val redacted: CellStyle = workbook.createCellStyle()
) {
    init {
        val headerFont = workbook.createFont()
        headerFont.bold = true
        header.setFontAndFillColor(headerFont, PALE_BLUE)

        val redactedFont = workbook.createFont()
        redactedFont.color = IndexedColors.RED.index
        redacted.setFontAndFillColor(redactedFont, GREY_25_PERCENT)
    }

    private fun CellStyle.setFontAndFillColor(font: Font, colour: IndexedColors) {
        this.setFont(font)
        this.setFillPattern(SOLID_FOREGROUND)
        this.fillForegroundColor = colour.index
    }
}

private data class Database(@JsonProperty("name") val name: String, @JsonProperty("jdbcUrl") val jdbcUrl: String, @JsonProperty("tables") val tables: Array<Table>)

private data class Table(@JsonProperty("name") val name: String, @JsonProperty("redact") val redact: Array<String> = emptyArray())

private fun String.isIn(values: Array<String>): Boolean = values.toList().contains(this)

