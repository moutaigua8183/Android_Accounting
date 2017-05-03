package com.moutaigua.accounting;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by mou on 1/26/17.
 */

public class ExcelHandler {

    private String filename;
    private Workbook workbook;



    public ExcelHandler(){
        workbook = null;
    }


    public void open(String local_file_name){
        filename = local_file_name;
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
        try {
            FileInputStream fis = new FileInputStream(file);
            workbook = WorkbookFactory.create(fis);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            FileOutputStream fileOut = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+filename);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void record(Transaction trans){
        Sheet sheet = workbook.getSheet( getSheetName(trans.date) );
        int lastRowNum = getLastEmptyRowIndex(sheet);
        Row row = sheet.createRow(lastRowNum);
        CellStyle charStyle = workbook.createCellStyle();
        charStyle.setAlignment(CellStyle.ALIGN_CENTER);
        CellStyle timeStyle = workbook.createCellStyle();
        timeStyle.setAlignment(CellStyle.ALIGN_CENTER);
        timeStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("m/d/yyyy"));
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setAlignment(CellStyle.ALIGN_CENTER);
        numberStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0.00"));

        Cell cell_time = row.createCell(0);
        cell_time.setCellStyle(timeStyle);
        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yy");
        try {
            Date date = sdf.parse(trans.date);
            cell_time.setCellValue( sdf.format(date) );
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Cell cell_loc = row.createCell(1);
        cell_loc.setCellStyle(charStyle);
        cell_loc.setCellValue(trans.location);

        Cell cell_what = row.createCell(2);
        cell_what.setCellStyle(charStyle);
        cell_what.setCellValue(trans.what);

        Cell cell_buyer = row.createCell(3);
        cell_buyer.setCellStyle(charStyle);
        cell_buyer.setCellValue(trans.buyer);

        Cell cell_payer = row.createCell(4);
        cell_payer.setCellStyle(charStyle);
        cell_payer.setCellValue(trans.payer);

        Cell cell_money = row.createCell(5);
        cell_money.setCellStyle(numberStyle);
        cell_money.setCellValue(trans.money);

        Cell cell_note = row.createCell(6);
        cell_note.setCellStyle(charStyle);
        cell_note.setCellValue(trans.note);
    }

    private String getSheetName(String date){
        String[] strings = date.split("/");
        return strings[2]+"."+strings[0];
    }

    private int getLastEmptyRowIndex(Sheet sheet){
        for(int i=0; ; i++){
            Cell timeTag = sheet.getRow(i).getCell(0);
            if(timeTag.toString().isEmpty()){
                return i;
            }
        }
    }

    public void uploadExistingServiceProviders() {
        FirebaseHandler firebaseHandler = new FirebaseHandler();
        Sheet sheet = workbook.getSheet("2017.01");
        int rowIndex = 0;
        while( !sheet.getRow(rowIndex).getCell(0).toString().isEmpty() ){
            String location = sheet.getRow(rowIndex).getCell(1).toString();
            if( location.equalsIgnoreCase("Chicago") ){
                FirebaseHandler.ServiceProvider provider = new FirebaseHandler.ServiceProvider();
                provider.name = sheet.getRow(rowIndex).getCell(2).toString();
                provider.location = "Chicago";
                firebaseHandler.addServiceProvider(provider);
            }
            ++rowIndex;
        }
    }



    static class Transaction {
        public String date;
        public String location;
        public String what;
        public String buyer;
        public String payer;
        public float money;
        public String note;
    }
}
