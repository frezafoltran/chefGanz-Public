package ai.picovoice.porcupine.demo;

import java.util.ArrayList;

/**
* This class defines the question objects used in building the user profile. Each question is stored in a Question object
* which allows peripheral properties to be added to it as well.
*/
public class ProfileQuestions {

    public ArrayList<Question> questions = new ArrayList<>();

    public ProfileQuestions(){

        this.questions.add(new Question("What's your name?", "username",
                null, "Name", "string"));

        this.questions.add(new Question("How old are you?",
                "user_age", null, "Age", "string"));


        String[] options1 = {"Dairy", "Eggs", "Nuts", "Shellfish", "Wheat", "Soy", "Fish"};
        this.questions.add(new Question("Do you have any food allergies?",
                "user_allergies", options1, "Allergies", "list"));

        String[] options2 = {"Intermittent Fasting", "Paleo", "Low-carb", "Whole30", "High-Protein", "Ketogenic"};
        this.questions.add(new Question("Do you follow any specific eating patterns?",
                "user_diet", options2, "Diet", "list"));


        String[] options3 = {"Cardiovascular Health", "Weight Loss/Management", "Energy"};
        this.questions.add(new Question("What specific health benefits are you " +
                "interested in from food?", "user_goals", options3, "Health goals",
                "list"));

        String[] options4 = {"Protein", "Vegetables", "Vitamins", "Minerals", "Fruits"};
        this.questions.add(new Question("What are you concerned you are lacking " +
                "in your diet?", "user_lacking_concerns", options4,
                "Want to eat more of", "list"));

        String[] options5 = {"Everyday", "Every other day", "Three times a week", "Twice a week", "Once a week", "Rarely"};
        this.questions.add(new Question("How many times a week do you exercise",
                "user_exercise_amount", options5, "Exercise routine",
                "list"));

        String[] options6 = {"Running", "Climbing", "Swimming", "Cycling", "Weight lifting", "None"};
        this.questions.add(new Question("What kind of exercising do you do?",
                "user_exercise_type", options6, "Sports practiced",
                "list"));


        String[] options7 = {"At most 15 min", "At most 30 min", "At most 45 min", "At most 1 hour", "No time limit"};
        this.questions.add(new Question("How much time to cook do you have per meal?",
                "user_cooking_time", options7, "Desired cooking time",
                "list"));


        // NOT ENOUGH INFO FOR NOW
        /*
        String[] options8 = {"At most 15 min", "At most 30 min", "At most 45 min", "At most 1 hour", "No time limit"};
        this.questions.add(new Question("What kind of cooking equipment do you have?", "user_cooking_equipment",
                options8));
                */

    }

    public class Question{

        public String questionText;
        public String answerAttribute;
        public String[] answerOptions;
        public String displayLabel;

        public String typeOfAnswer;
        //TODO other attributes to discuss with Kurt

        public Question(String questionText, String answerAttribute,
                        String[] answerOptions, String displayLabel, String typeOfAnswer){
            this.questionText = questionText;
            this.answerAttribute = answerAttribute;
            this.answerOptions = answerOptions;
            this.displayLabel = displayLabel;
            this.typeOfAnswer = typeOfAnswer;
        }
    }
}
