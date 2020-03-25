package ai.picovoice.porcupine.demo;

public class IngredientData {

    String name;
    String count;
    String unit;
    String display_name;

    public IngredientData(String name, String count, String unit) {

        this.display_name = name.replace("_", " ");
        this.name = name.trim();
        this.count = count.trim();
        this.unit = unit.trim();
    }
}
