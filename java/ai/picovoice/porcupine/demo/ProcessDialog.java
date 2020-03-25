package ai.picovoice.porcupine.demo;

import java.util.HashSet;

/**
* This class takes care of the language processing that can be done locally. Most of the text from user input is sent
* to dialogflow for processing, but for some simple tasks, it is worth it to do this processing locally for speed.
*/
public class ProcessDialog {

    public static HashSet<String> nextStepSyns(){

        HashSet<String> set = new HashSet<String>();
        set.add("next step");
        set.add("go to next step");

        return set;
    }
    public static HashSet<String> previousStepSyns(){

        HashSet<String> set = new HashSet<String>();
        set.add("previous step");
        set.add("go to previous step");

        return set;
    }
    public static HashSet<String> repeatStepSyns(){

        HashSet<String> set = new HashSet<String>();
        set.add("repeat step");
        set.add("repeat last step");
        set.add("repeat this step");

        return set;
    }
    public static HashSet<String> pauseStepSyns(){

        HashSet<String> set = new HashSet<String>();
        set.add("pause");
        set.add("pause steps");
        set.add("pause step");
        set.add("pause instruction");
        set.add("stop");
        set.add("stop speaking");
        set.add("shut up");

        return set;
    }

}
