package ai.picovoice.porcupine.demo;

/**
* This class organizes the data to be used by ListAdapters (CustomAdapter for instance)
*/
class SubjectData {
    String SubjectName;
    String Image;
    String id;

    public SubjectData(String subjectName, String id, String image) {
        this.SubjectName = subjectName;
        this.Image = image;
        this.id = id;
    }

}
