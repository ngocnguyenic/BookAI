package model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


 
public class QA {
    
    private int QAID;
    private int chapterID;
    private String question;
    private String answer;
    private String difficulty;        
    private String questionType;      
    private String bloomLevel;        
    private String questionTypeTag;   
    private boolean autoTagged;       
    private boolean vectorIndexed;    
    private Timestamp updatedAt;      
    

    private List<Tag> tags;           
    private VectorMetadata vectorMetadata; 
    

    public QA() {
        this.tags = new ArrayList<>();
        this.autoTagged = false;
        this.vectorIndexed = false;
    }
    
    public QA(int chapterID, String question, String answer, String difficulty, String questionType) {
        this();
        this.chapterID = chapterID;
        this.question = question;
        this.answer = answer;
        this.difficulty = difficulty;
        this.questionType = questionType;
    }
    

    public QA(int chapterID, String question, String answer, String difficulty, String questionType,
              String bloomLevel, String questionTypeTag) {
        this(chapterID, question, answer, difficulty, questionType);
        this.bloomLevel = bloomLevel;
        this.questionTypeTag = questionTypeTag;
    }
    
    public int getQAID() { return QAID; }
    public void setQAID(int QAID) { this.QAID = QAID; }
    
    public int getChapterID() { return chapterID; }
    public void setChapterID(int chapterID) { this.chapterID = chapterID; }
    
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    
    public String getBloomLevel() { return bloomLevel; }
    public void setBloomLevel(String bloomLevel) { this.bloomLevel = bloomLevel; }
    
    public String getQuestionTypeTag() { return questionTypeTag; }
    public void setQuestionTypeTag(String questionTypeTag) { this.questionTypeTag = questionTypeTag; }
    
    public boolean isAutoTagged() { return autoTagged; }
    public void setAutoTagged(boolean autoTagged) { this.autoTagged = autoTagged; }
    
    public boolean isVectorIndexed() { return vectorIndexed; }
    public void setVectorIndexed(boolean vectorIndexed) { this.vectorIndexed = vectorIndexed; }
    
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    
    // Related objects
    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tags) { this.tags = tags; }
    public void addTag(Tag tag) { this.tags.add(tag); }
    
    public VectorMetadata getVectorMetadata() { return vectorMetadata; }
    public void setVectorMetadata(VectorMetadata vectorMetadata) { this.vectorMetadata = vectorMetadata; }
    
    public boolean isFullyProcessed() {
        return autoTagged && vectorIndexed;
    }
    

    public String getDifficultyDisplay() {
        if (difficulty == null) return "❓ Unknown";
        return switch (difficulty.toLowerCase()) {
            case "easy" -> "🟢 Easy";
            case "medium" -> "🟡 Medium";
            case "hard" -> "🔴 Hard";
            default -> "❓ " + difficulty;
        };
    }
    
    public String getBloomLevelDisplay() {
        if (bloomLevel == null) return "❓ Unknown";
        return switch (bloomLevel.toLowerCase()) {
            case "remember" -> "🧠 Remember";
            case "understand" -> "💡 Understand";
            case "apply" -> "🔧 Apply";
            case "analyze" -> "🔍 Analyze";
            case "evaluate" -> "⚖️ Evaluate";
            case "create" -> "✨ Create";
            default -> "❓ " + bloomLevel;
        };
    }
    public String getProcessingStatus() {
        if (!autoTagged && !vectorIndexed) {
            return "⏳ Pending";
        } else if (autoTagged && !vectorIndexed) {
            return "🏷️ Tagged";
        } else if (autoTagged && vectorIndexed) {
            return "✅ Complete";
        } else {
            return "⚠️ Partial";
        }
    }
    
    public String getTagsString() {
        if (tags == null || tags.isEmpty()) {
            return "No tags";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(tags.get(i).getTagName());
        }
        return sb.toString();
    }
    
    public List<Tag> getTagsByType(String tagType) {
        List<Tag> result = new ArrayList<>();
        if (tags != null) {
            for (Tag tag : tags) {
                if (tagType.equalsIgnoreCase(tag.getTagType())) {
                    result.add(tag);
                }
            }
        }
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("QA[ID=%d, Q='%s...', Difficulty=%s, Bloom=%s, Tagged=%b, Indexed=%b]",
            QAID,
            question != null ? question.substring(0, Math.min(30, question.length())) : "null",
            difficulty,
            bloomLevel,
            autoTagged,
            vectorIndexed
        );
    }
    
    public static class Tag {
        private int tagID;
        private String tagName;
        private String tagType;
        private String description;
        private float confidence;
        
        public Tag() {}
        
        public Tag(String tagName, String tagType) {
            this.tagName = tagName;
            this.tagType = tagType;
        }
        
        public int getTagID() { return tagID; }
        public void setTagID(int tagID) { this.tagID = tagID; }
        
        public String getTagName() { return tagName; }
        public void setTagName(String tagName) { this.tagName = tagName; }
        
        public String getTagType() { return tagType; }
        public void setTagType(String tagType) { this.tagType = tagType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public float getConfidence() { return confidence; }
        public void setConfidence(float confidence) { this.confidence = confidence; }
        
        public String getDisplay() {
            String prefix = "";
            if (tagType != null) {
                switch (tagType.toLowerCase()) {
                    case "topic" -> prefix = "📚 ";
                    case "concept" -> prefix = "💡 ";
                    case "bloom_level" -> prefix = "🧠 ";
                    case "question_type" -> prefix = "❓ ";
                }
            }
            return prefix + tagName;
        }
        
        @Override
        public String toString() {
            return String.format("Tag[%s:%s]", tagType, tagName);
        }
    }
    
    public static class VectorMetadata {
        private int vectorID;
        private int qaID;
        private String embeddingModel;
        private int dimension;
        private String vectorChecksum;
        private Timestamp indexedAt;
        
        public VectorMetadata() {}
        
        public int getVectorID() { return vectorID; }
        public void setVectorID(int vectorID) { this.vectorID = vectorID; }
        
        public int getQaID() { return qaID; }
        public void setQaID(int qaID) { this.qaID = qaID; }
        
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
        
        public String getVectorChecksum() { return vectorChecksum; }
        public void setVectorChecksum(String vectorChecksum) { this.vectorChecksum = vectorChecksum; }
        
        public Timestamp getIndexedAt() { return indexedAt; }
        public void setIndexedAt(Timestamp indexedAt) { this.indexedAt = indexedAt; }
        
        @Override
        public String toString() {
            return String.format("Vector[Model=%s, Dim=%d]", embeddingModel, dimension);
        }
    }
}