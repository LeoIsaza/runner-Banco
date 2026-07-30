package cl.bice.createStructure.utils;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    @SneakyThrows
    public static String printJsonModel(Object val) {
        JsonMapper json = new JsonMapper();
        json.registerModule(new JavaTimeModule());
        return json.writeValueAsString(val);
    }

    public static int currentRut=0;
    public static int thread=0;

    public static String getRandomRut() {
        int rut = Integer.parseInt(DateTimeFormatter.ofPattern("MMmmssSSS").format(LocalDateTime.now()).substring(1));
        currentRut=currentRut==0?rut+thread:currentRut;
        thread=thread>99?0:thread+1;
        rut= currentRut==rut?rut+thread:rut;
        currentRut=rut;
        char[] num = String.valueOf(rut).toCharArray();
        int cont = 1;
        int suma = 0;
        for (int i = 0; i <= num.length - 1; i++) {
            cont = cont == 7 ? 2 : cont + 1;
            suma = suma + Integer.parseInt(String.valueOf(num[num.length - 1 - i])) * cont;
        }
        int value = 11 - Math.abs((suma - ((suma / 11) * 11)));
        String digit = value == 11 ? "0" : value == 10 ? "K" : String.valueOf(value);
        return rut + digit;
    }
}
