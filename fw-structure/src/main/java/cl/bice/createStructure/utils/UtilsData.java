package cl.bice.createStructure.utils;

import com.github.javafaker.Faker;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Objects;

public class UtilsData {

    private final Faker faker = new Faker();

    public String getString(int minLength, int maxLength) {
        return subStringValor(faker.lorem().sentence(), minLength, maxLength, true);
    }

    private String subStringValor(String valor, int minLength, int maxLength, boolean removesSpecialCharacters) {

        try {
            if (Objects.isNull(valor)) {
                return "";
            }

            if (removesSpecialCharacters) {
                valor = valor.replaceAll("[^a-zA-Z]", "");
            }

            if (valor.length() >= maxLength) {
                return valor.substring(0, maxLength - minLength);
            }

            return valor;
        } catch (Exception ex){
            System.out.println(ex.getMessage());
            return RandomStringUtils.randomAlphanumeric(17);
        }
    }
}
