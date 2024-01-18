package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@RestController
@RequestMapping
public class RisTransformer {

    @Value("${FILEPATH}")
    private static String FILEPATH;

    @PostMapping ("/uploadRis")
    public void readRisFile(@RequestParam("file") MultipartFile multipartFile ) throws IOException{
        File file = transferToFile(multipartFile);
        String csvTitle = "\"Authors\",\"Title\",\"Years\",\"Source title\"," +
                "\"Volume\",\"Issue\",\"Page start\",\"Page end\",\"DOI\",\"Link\",\"Abstract\",\"Author Keywords\",\"Publisher\"," +
                "\"ISSN\",\"ISBN\",\"Language of Original Document\",\"Document Type\",\"Publication Stage\",\"Source\"\n";
        String csvBody = "";
        List<JSONObject> publicationlist = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(file);

            JSONObject publication = new JSONObject();
            ArrayList<String> authorList = new ArrayList<>();
            ArrayList<String> keywordList = new ArrayList<>();
            while (scanner.hasNextLine()) {

                String line = scanner.nextLine();
                System.out.println(line);

                if(line.contains("TY  - ")){
                    publication.put("Type",",\"" + line.substring(6) + "\"");
                }else if (line.contains("T1  - ")){
                    publication.put("PrimaryTitle",",\"" + line.substring(6) + "\"");
                }else if (line.contains("AU  - ")) {
                    authorList.add( line.substring(6) ) ;
                }else if(line.contains("Y1  - ")) {
                    publication.put("PrimaryDate",",\"" + line.substring(6) + "\"");
                }else if(line.contains("PY  - ")) {
                    publication.put("PublicationYear",",\"" + line.substring(6) + "\"");
                }else if(line.contains("DA  - ")) {
                    publication.put("Date",",\"" + line.substring(6) + "\"");
                }else if(line.contains("DO  - ")) {
                    publication.put("DOI",",\"" + line.substring(6) + "\"");
                }else if(line.contains("T2  - ")) {
                    publication.put("SecondaryTitle",",\"" + line.substring(6) + "\"");
                }else if(line.contains("JF  - ")) {
                    publication.put("Journal/PeriodicalName",",\"" + line.substring(6) + "\"");
                }else if(line.contains("JO  - ")) {
                    publication.put("Journal/PeriodicalName",",\"" + line.substring(6) + "\"");
                }else if(line.contains("JA  - ")) {
                    publication.put("PeriodicalName",",\"" + line.substring(6) + "\"");
                }else if(line.contains("SP  - ")) {
                    publication.put("Page Start",",\"" + line.substring(6) + "\"");
                }else if(line.contains("EP  - ")) {
                    publication.put("Page End",",\"" + line.substring(6) + "\"");
                }else if(line.contains("VL  - ")) {
                    publication.put("Volume",",\"" + line.substring(6) + "\"");
                }else if(line.contains("IS  - ")) {
                    publication.put("Issue",",\"" + line.substring(6) + "\"");
                }else if(line.contains("KW  - ")) {
                    keywordList.add( line.substring(6) ) ;
                }else if(line.contains("PB  - ")) {
                    publication.put("Publisher",",\"" + line.substring(6) + "\"");
                }else if(line.contains("SN  - ")) {
                    publication.put("ISBN/ISSN",",\"" + line.substring(6) + "\"");
                }else if(line.contains("UR  - ")) {
                    publication.put("URL",",\"" + line.substring(6) + "\"");
                }else if(line.contains("Y2  - ")) {
                    publication.put("Today",",\"" + line.substring(6) + "\"");
                }else if(line.contains("N1  - ")) {
                    publication.put("Journal/PeriodicalName",",\"" + line.substring(6) + "\"");
                }else if(line.contains("N2  - ")) {
                    publication.put("Abstract",",\"" + line.substring(6) + "\"");
                }else if(line.contains("ER  - ")) {
                    if(authorList.size()>0){
                        String fullAuthor =  "\"";
                        for(int i=0;i<authorList.size();i++){
                            if(i==0){
                                fullAuthor = fullAuthor + authorList.get(i);
                            }else{
                                fullAuthor = fullAuthor + ";" + authorList.get(i);
                            }
                        }
                        publication.put("Authors",fullAuthor+"\"");
                    }else{
                        publication.put("Authors","\"\"");
                    }

                    if(keywordList.size()>0){
                        String fullKeyword =  "\"";
                        for(int i=0;i<keywordList.size();i++){
                            if(i==0){
                                fullKeyword = fullKeyword + keywordList.get(i);
                            }else{
                                fullKeyword = fullKeyword + ";" + keywordList.get(i);
                            }
                        }
                        publication.put("Keywords",fullKeyword+"\"");
                    }else{
                        publication.put("Keywords","\"\"");
                    }
                    publicationlist.add(publication);
                    publication = new JSONObject();
                    authorList = new ArrayList<>();
                    keywordList = new ArrayList<>();
                }

            }
            scanner.close();
            System.out.println(publicationlist);



        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //生成Csv文件
        //"\"Authors\",\"Title\",\"Years\",\"Source title\"," +
        //                "\"Volume\",\"Issue\",\"Page start\",\"Page end\",\"DOI\",\"Link\",\"Abstract\",\"Author Keywords\",\"Publisher\"," +
        //                "\"ISSN\",\"ISBN\",\"Language of Original Document\",\"Document Type\",\"Publication Stage\",\"Source\"\n";
        for(int i=0;i<publicationlist.size();i++){
            if(publicationlist.get(i).get("Type").equals(",\"JOUR\"")){
                csvBody = csvBody + generateField(publicationlist.get(i),"Authors") + generateField(publicationlist.get(i),"PrimaryTitle")
                        + generateField(publicationlist.get(i),"PublicationYear")+ generateField(publicationlist.get(i),"Journal/PeriodicalName")
                        + generateField(publicationlist.get(i),"Volume") + generateField(publicationlist.get(i),"Issue")
                        + generateField(publicationlist.get(i),"Page Start")+ generateField(publicationlist.get(i),"Page End")
                        + generateField(publicationlist.get(i),"DOI") + generateField(publicationlist.get(i),"URL")
                        + generateField(publicationlist.get(i),"Abstract")+ ","+generateField(publicationlist.get(i),"Keywords") + generateField(publicationlist.get(i),"Publisher")
                        + generateField(publicationlist.get(i),"ISBN/ISSN")+",,\"English\",\"JOUR\",\"Final\",\"Wiley Online Library\"\n";
            }else if(publicationlist.get(i).get("Type").equals(",\"CHAP\"")){
                csvBody = csvBody + generateField(publicationlist.get(i),"Authors") + generateField(publicationlist.get(i),"PrimaryTitle")
                        + generateField(publicationlist.get(i),"PublicationYear")+ generateField(publicationlist.get(i),"Journal/PeriodicalName")
                        + generateField(publicationlist.get(i),"Volume") + generateField(publicationlist.get(i),"Issue")
                        + generateField(publicationlist.get(i),"Page Start")+ generateField(publicationlist.get(i),"Page End")
                        + generateField(publicationlist.get(i),"DOI") + generateField(publicationlist.get(i),"URL")
                        + generateField(publicationlist.get(i),"Abstract")+","+generateField(publicationlist.get(i),"Keywords") + generateField(publicationlist.get(i),"Publisher")
                        + ","  + generateField(publicationlist.get(i),"ISBN/ISSN") +",\"English\",\"CHAP\",\"Final\",\"Wiley Online Library\"\n";
            }
        }
//        // 创建文件
//        File csvFile = new File("C:\\Users\\10421\\Desktop\\a.csv");
//        FileWriter writer = new FileWriter(csvFile, true);
//        writer.write((csvTitle+csvBody).toString());
//        // 关闭文件
//        writer.close();

        try{
            File csvFile = new File("D:\\文档资料\\05论文\\LiteratureReview2023\\DT+Engineering\\转换结果.csv");
            FileOutputStream fos = new FileOutputStream(csvFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write((csvTitle+csvBody));
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String generateField(JSONObject fieldJSON, String fieldToFound){

        //String returnVal = "";
        if(fieldJSON.get(fieldToFound)==null){
            return ",\"\"";
        }else{
            return fieldJSON.getString(fieldToFound);
        }

    }

    @GetMapping("/csvLocation")
    public String get() throws IOException {
        return "D:\\文档资料\\05论文\\LiteratureReview2023\\DT+Engineering\\转换结果.csv";
    }

    public File transferToFile(MultipartFile multipartFile) {

        File file = null;
        try {
            String originalFilename = multipartFile.getOriginalFilename();
            String[] filename = originalFilename.split("\\.");
            file=File.createTempFile(filename[0], filename[1]);
            multipartFile.transferTo(file);
            file.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
    public void risToCSV(){

    }



    /**
     * 生成CSV文件
     */
    @PostMapping("/hello")
    private static void writeToResource() throws IOException {
        String data = "\"Author\",\"Title\",\"Year\"\n\"Shouxuan Wu\",\"Cognitive Thread\",\"2020\"";

        // 创建文件
        File file = new File("C:\\Users\\10421\\Desktop\\a.csv");
        FileWriter writer = new FileWriter(file, true);


        writer.write(data.toString());


        // 关闭文件
        writer.close();

    }








    @GetMapping("/download")
    public void downloadCSV(HttpServletResponse response) throws IOException {
            String fileName = "scopus.csv";
            ClassPathResource resource = new ClassPathResource(fileName);
            InputStream is = null;
            try {
                is = resource.getInputStream();
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                response.setHeader("Content-Disposition", "attachment; filename=\"" +
                        new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1) + "\"");
                OutputStream os = response.getOutputStream();
                FileCopyUtils.copy(is, os);
            } catch (IOException e) {
                e.printStackTrace();
            }

    }


}
