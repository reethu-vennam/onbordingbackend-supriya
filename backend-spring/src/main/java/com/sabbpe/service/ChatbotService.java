package com.sabbpe.service;

import com.sabbpe.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ChatbotService {

    private static final Pattern PAN_PATTERN = Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]{1}\\b");
    private static final Pattern GST_PATTERN = Pattern.compile("\\b[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}\\b");
    private static final Pattern IFSC_PATTERN = Pattern.compile("\\b[A-Za-z]{4}[0-9]{7}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b[6-9][0-9]{9}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("\\b[0-9]{9,18}\\b");
    private static final Pattern PINCODE_PATTERN = Pattern.compile("\\b[1-9][0-9]{5}\\b");
    private static final Pattern CONFIRM_YES_PATTERN = Pattern.compile("\\b(yes|yeah|yep|sure|ok|okay|ready|go ahead|proceed|start|begin|continue|next|y)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONFIRM_NO_PATTERN = Pattern.compile("\\b(no|nope|not|skip|n)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PURE_YES_NO = Pattern.compile("^\\s*(yes|yeah|yep|no|nope|y|n)\\s*[.!?]*\\s*$", Pattern.CASE_INSENSITIVE);

    private final Map<String, ConversationState> sessions = new ConcurrentHashMap<>();

    private static class ConversationState {
        String sessionId;
        String currentStep;
        String language;
        int questionIndex;
        List<StepQuestion> questions;
        Map<String, Object> collectedData;
        StateType state;
        boolean needsHint;

        ConversationState(String sessionId, String currentStep, String language) {
            this.sessionId = sessionId;
            this.currentStep = currentStep;
            this.language = language != null ? language : "en";
            this.collectedData = new LinkedHashMap<>();
            this.state = StateType.GREET;
            initStep(currentStep);
        }

        void initStep(String step) {
            this.currentStep = step;
            this.questions = QuestionsByStep.getQuestions(step);
            this.questionIndex = -1;
            if (questions.isEmpty()) {
                this.state = StateType.COMPLETED;
            } else {
                this.state = StateType.GREET;
            }
        }

        StepQuestion currentQuestion() {
            if (questionIndex >= 0 && questionIndex < questions.size()) {
                return questions.get(questionIndex);
            }
            return null;
        }

        boolean hasMoreQuestions() {
            return questionIndex + 1 < questions.size();
        }

        StepQuestion nextQuestion(Map<String, Object> collectedData) {
            questionIndex++;
            while (questionIndex < questions.size()) {
                StepQuestion q = questions.get(questionIndex);
                if (shouldSkip(q, collectedData)) {
                    questionIndex++;
                    continue;
                }
                return q;
            }
            return null;
        }

        boolean shouldSkip(StepQuestion q, Map<String, Object> collectedData) {
            if (q.fieldKey.equals("gstNumber")) {
                Object hasGST = collectedData.get("hasGST");
                return hasGST != null && "false".equals(String.valueOf(hasGST));
            }
            return false;
        }
    }

    enum StateType {
        GREET,
        AWAITING_CONFIRM,
        ASKING,
        COMPLETED
    }

    static class StepQuestion {
        String fieldKey;
        String prompt;
        boolean optional;

        StepQuestion(String fieldKey, String prompt, boolean optional) {
            this.fieldKey = fieldKey;
            this.prompt = prompt;
            this.optional = optional;
        }
    }

    static class QuestionsByStep {
        static final Map<String, List<StepQuestion>> STEP_QUESTIONS = new LinkedHashMap<>();

        static {
            STEP_QUESTIONS.put("entity-type", List.of(
                new StepQuestion("entityType",
                    "What type of business entity do you have? Choose from:\n" +
                    "• Proprietorship\n• Individual\n• Partnership\n• LLP\n• Pvt Ltd\n• Public Ltd\n• Trust\n• Society\n• HUF\n• Government/PSU\n• Education",
                    false)
            ));

            STEP_QUESTIONS.put("products", List.of(
                new StepQuestion("products",
                    "Which payment products would you like to enable? For example:\n" +
                    "• UPI QR\n• Payment Gateway\n• Payment Links\n• Smart POS\n• Subscription\n• Split Payments",
                    false)
            ));

            STEP_QUESTIONS.put("business-details", List.of(
                new StepQuestion("businessName",
                    "What is the name of your business / entity?",
                    false),
                new StepQuestion("hasGST",
                    "Is your business GST registered? (yes/no)",
                    false),
                new StepQuestion("gstNumber",
                    "What is your GST number?",
                    true),
                new StepQuestion("addressLine1",
                    "What is your registered address? (Street / Building / Area)",
                    false),
                new StepQuestion("city",
                    "Which city is your business in?",
                    false),
                new StepQuestion("pincode",
                    "What is the pincode? (6 digits)",
                    false),
                new StepQuestion("state",
                    "Which state? (e.g., Karnataka, Maharashtra, Delhi)",
                    false)
            ));

            STEP_QUESTIONS.put("person-kyc", List.of(
                new StepQuestion("panNumber",
                    "What is your PAN number? (e.g., ABCDE1234F)",
                    false),
                new StepQuestion("aadhaarNumber",
                    "What is your Aadhaar number? (12 digits)",
                    true)
            ));

            STEP_QUESTIONS.put("bank-details", List.of(
                new StepQuestion("accountHolderName",
                    "What is the bank account holder's name?",
                    false),
                new StepQuestion("accountNumber",
                    "What is your bank account number?",
                    false),
                new StepQuestion("confirmAccountNumber",
                    "Please re-enter your bank account number to confirm.",
                    false),
                new StepQuestion("ifscCode",
                    "What is the IFSC code of your bank branch? (e.g., SBIN0001234)",
                    false),
                new StepQuestion("bankName",
                    "What is your bank name? (e.g., State Bank of India, HDFC Bank)",
                    false)
            ));

            STEP_QUESTIONS.put("doing-business", List.of(
                new StepQuestion("operatingAddressDifferent",
                    "Is your operating address different from your registered address? (yes/no)",
                    false)
            ));
        }

        static List<StepQuestion> getQuestions(String step) {
            return STEP_QUESTIONS.getOrDefault(step, Collections.emptyList());
        }
    }

    public ChatResponse processMessage(String sessionId, String message, String currentStep, String language) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        ConversationState state = sessions.get(sessionId);

        if (state == null || !Objects.equals(state.currentStep, currentStep)) {
            state = new ConversationState(sessionId, currentStep, language);
            sessions.put(sessionId, state);
        } else if (state.state == StateType.COMPLETED) {
            state.initStep(currentStep);
        }

        if (message == null) {
            message = "";
        }
        message = message.trim();

        if (!message.isEmpty()) {
            processUserMessage(state, message);
        }

        ChatResponse response = buildResponse(state);
        log.debug("Chatbot response for session {} step {}: reply='{}', stepComplete={}",
                sessionId, currentStep, response.getReply(), response.isStepComplete());

        if (state.state == StateType.COMPLETED) {
            sessions.remove(sessionId);
        }

        return response;
    }

    private void processUserMessage(ConversationState state, String message) {
        switch (state.state) {
            case GREET -> {
                StepQuestion firstQ = state.questions.get(0);
                if (firstQ != null) {
                    String extracted = extractFieldValue(firstQ.fieldKey, message);
                    if (extracted != null && !extracted.isBlank()) {
                        state.collectedData.put(firstQ.fieldKey, extracted);
                    } else if (isPureYesNo(message) && needsRealText(firstQ.fieldKey)) {
                        state.needsHint = true;
                        break;
                    } else if (!isPureYesNo(message)) {
                        state.collectedData.put(firstQ.fieldKey, message.trim());
                    } else {
                        break;
                    }
                }
                state.questionIndex = 0;
                if (state.hasMoreQuestions()) {
                    state.nextQuestion(state.collectedData);
                    state.state = state.currentQuestion() != null ? StateType.ASKING : StateType.COMPLETED;
                } else {
                    state.state = StateType.COMPLETED;
                }
            }
            case AWAITING_CONFIRM -> {
                boolean isConfirm = CONFIRM_YES_PATTERN.matcher(message).find();
                if (isConfirm) {
                    state.nextQuestion(state.collectedData);
                    state.state = state.currentQuestion() != null ? StateType.ASKING : StateType.COMPLETED;
                }
            }
            case ASKING -> {
                StepQuestion q = state.currentQuestion();
                if (q != null) {
                    if (isPureYesNo(message) && needsRealText(q.fieldKey)) {
                        state.needsHint = true;
                        break;
                    }

                    String extracted = extractFieldValue(q.fieldKey, message);
                    if (extracted != null && !extracted.isBlank()) {
                        state.collectedData.put(q.fieldKey, extracted);
                    } else if (!needsRealText(q.fieldKey) || !isPureYesNo(message)) {
                        state.collectedData.put(q.fieldKey, message.trim());
                    } else {
                        break;
                    }

                    if (state.hasMoreQuestions()) {
                        state.nextQuestion(state.collectedData);
                        state.state = state.currentQuestion() != null ? StateType.ASKING : StateType.COMPLETED;
                    } else {
                        state.state = StateType.COMPLETED;
                    }
                } else {
                    state.state = StateType.COMPLETED;
                }
            }
            default -> state.state = StateType.COMPLETED;
        }
    }

    private boolean questionsInMessage(ConversationState state, String message) {
        if (state.questions.isEmpty()) return true;
        StepQuestion q = state.questions.get(0);
        if (q == null) return false;
        String extracted = extractFieldValue(q.fieldKey, message);
        return extracted != null && !extracted.isBlank();
    }

    private boolean isPureYesNo(String message) {
        return PURE_YES_NO.matcher(message).matches();
    }

    private boolean needsRealText(String fieldKey) {
        return switch (fieldKey) {
            case "businessName", "addressLine1", "city", "state",
                 "accountHolderName", "bankName", "branchName",
                 "fullName", "panNumber" -> true;
            default -> false;
        };
    }

    private String extractFieldValue(String fieldKey, String message) {
        switch (fieldKey) {
            case "panNumber" -> {
                Matcher m = PAN_PATTERN.matcher(message.toUpperCase());
                if (m.find()) return m.group();
            }
            case "gstNumber" -> {
                Matcher m = GST_PATTERN.matcher(message.toUpperCase());
                if (m.find()) return m.group();
            }
            case "mobileNumber" -> {
                Matcher m = PHONE_PATTERN.matcher(message);
                if (m.find()) return m.group();
            }
            case "email" -> {
                Matcher m = EMAIL_PATTERN.matcher(message);
                if (m.find()) return m.group();
            }
            case "accountNumber" -> {
                Matcher m = ACCOUNT_NUMBER_PATTERN.matcher(message);
                if (m.find()) return m.group();
            }
            case "ifscCode" -> {
                Matcher m = IFSC_PATTERN.matcher(message.toUpperCase());
                if (m.find()) return m.group();
            }
            case "aadhaarNumber" -> {
                Matcher m = Pattern.compile("\\b[0-9]{12}\\b").matcher(message);
                if (m.find()) return m.group();
            }
            case "hasGST" -> {
                if (CONFIRM_YES_PATTERN.matcher(message).find()) return "true";
                if (CONFIRM_NO_PATTERN.matcher(message).find()) return "false";
            }
            case "operatingAddressDifferent" -> {
                if (CONFIRM_YES_PATTERN.matcher(message).find()) return "true";
                if (CONFIRM_NO_PATTERN.matcher(message).find()) return "false";
            }
            case "entityType" -> {
                return extractEntityType(message);
            }
            case "products" -> {
                return message;
            }
            case "pincode" -> {
                Matcher m = PINCODE_PATTERN.matcher(message);
                if (m.find()) return m.group();
            }
            case "state" -> {
                return extractState(message);
            }
            case "city" -> {
                return extractCity(message);
            }
        }
        return null;
    }

    private String extractEntityType(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("proprietor")) return "proprietorship";
        if (lower.contains("individual") || lower.contains("single") || lower.contains("person")) return "individual";
        if (lower.contains("partnership") || lower.contains("partner")) return "partnership";
        if (lower.contains("llp")) return "llp";
        if (lower.contains("private") || lower.contains("pvt") || lower.contains("pvt ltd") || lower.contains("pvt. ltd")) return "pvt_ltd";
        if (lower.contains("public") || lower.contains("public ltd")) return "public_ltd";
        if (lower.contains("trust")) return "trust";
        if (lower.contains("society") || lower.contains("soc")) return "society";
        if (lower.contains("huf")) return "huf";
        if (lower.contains("government") || lower.contains("govt") || lower.contains("psu")) return "government_psu";
        if (lower.contains("education") || lower.contains("school") || lower.contains("college") || lower.contains("university")) return "education";
        return lower;
    }

    private static final List<String> INDIAN_STATES = List.of(
        "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
        "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka",
        "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram",
        "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu",
        "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal",
        "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu",
        "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
    );

    private String extractState(String message) {
        String trimmed = message.trim();
        String lower = trimmed.toLowerCase();

        for (String state : INDIAN_STATES) {
            if (lower.contains(state.toLowerCase())) {
                return state;
            }
        }

        String cleaned = lower
            .replaceAll("^(state|st|is|of|in|from|the|my)\\s+", "")
            .replaceAll("\\s+(state|st)$", "")
            .trim();

        for (String state : INDIAN_STATES) {
            if (cleaned.equals(state.toLowerCase())) {
                return state;
            }
        }

        return trimmed;
    }

    private String extractCity(String message) {
        String trimmed = message.trim();
        String lower = trimmed.toLowerCase();

        String cleaned = lower
            .replaceAll("^(city|town|location|area|is|of|in|from|the|my)\\s+", "")
            .replaceAll("\\s+(city|town)$", "")
            .trim();

        if (!cleaned.isEmpty()) {
            return cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
        }
        return trimmed;
    }

    private ChatResponse buildResponse(ConversationState state) {
        StepQuestion currentQ = state.currentQuestion();
        StepQuestion nextQ = state.hasMoreQuestions() ? state.questions.get(state.questionIndex + 1) : null;

        String reply = switch (state.state) {
            case GREET -> buildGreeting(state);
            case ASKING -> buildQuestionReply(state, currentQ);
            case AWAITING_CONFIRM -> buildAwaitingReply(state, currentQ);
            case COMPLETED -> buildCompletedReply(state);
            default -> "Hello! How can I help you with your onboarding?";
        };

        return ChatResponse.builder()
                .sessionId(state.sessionId)
                .reply(reply)
                .currentStep(state.currentStep)
                .currentQuestion(currentQ != null ? currentQ.fieldKey : null)
                .questionIndex(currentQ != null ? state.questionIndex : -1)
                .totalQuestions(state.questions.size())
                .stepComplete(state.state == StateType.COMPLETED)
                .collectedData(state.state == StateType.COMPLETED ? new LinkedHashMap<>(state.collectedData) : null)
                .build();
    }

    private String buildGreeting(ConversationState state) {
        if (state.questions.isEmpty()) {
            return "This step's already done! You can move on to the next one.";
        }

        StepQuestion firstQ = state.questions.get(0);
        String stepName = getStepDisplayName(state.currentStep);

        if (state.needsHint) {
            state.needsHint = false;
            return "Hey! I need the actual value here, not yes or no.\n\n" + firstQ.prompt;
        }

        return String.format("Hey! Let's take care of the **%s** section.\n\n%s",
                stepName, firstQ.prompt);
    }

    private String buildQuestionReply(ConversationState state, StepQuestion q) {
        if (q == null) {
            return "What else can you tell me?";
        }
        if (state.needsHint) {
            state.needsHint = false;
            return "I need the actual value here, not yes or no.\n\n" + q.prompt;
        }
        return q.prompt;
    }

    private String buildAwaitingReply(ConversationState state, StepQuestion q) {
        if (q != null) {
            return q.prompt;
        }
        return "Ready for the next one?";
    }

    private String buildCompletedReply(ConversationState state) {
        if (state.collectedData.isEmpty()) {
            return "All done! You can move on to the next step.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Got it! Here's what you told me:\n\n");

        Map<String, String> fieldLabels = getFieldLabels(state.currentStep);
        for (Map.Entry<String, Object> entry : state.collectedData.entrySet()) {
            String label = fieldLabels.getOrDefault(entry.getKey(), entry.getKey());
            sb.append("• ").append(label).append(": ").append(entry.getValue()).append("\n");
        }

        sb.append("\nI've filled all that in for you. Give it a quick look and move on when you're ready!");
        return sb.toString();
    }

    private String getStepDisplayName(String step) {
        return switch (step) {
            case "entity-type" -> "Entity Type";
            case "products" -> "Products";
            case "business-details" -> "Business Details";
            case "person-kyc" -> "Person KYC";
            case "entity-documents" -> "Entity Documents";
            case "doing-business" -> "Address Proof";
            case "bank-details" -> "Bank Details";
            case "kyc" -> "KYC Verification";
            case "review" -> "Review & Submit";
            default -> "Onboarding";
        };
    }

    private Map<String, String> getFieldLabels(String step) {
        return switch (step) {
            case "business-details" -> Map.ofEntries(
                    Map.entry("businessName", "Business Name"),
                    Map.entry("hasGST", "GST Registered"),
                    Map.entry("gstNumber", "GST Number"),
                    Map.entry("addressLine1", "Address"),
                    Map.entry("city", "City"),
                    Map.entry("pincode", "Pincode"),
                    Map.entry("state", "State")
            );
            case "person-kyc" -> Map.ofEntries(
                    Map.entry("panNumber", "PAN Number"),
                    Map.entry("aadhaarNumber", "Aadhaar Number")
            );
            case "bank-details" -> Map.ofEntries(
                    Map.entry("accountHolderName", "Account Holder"),
                    Map.entry("accountNumber", "Account Number"),
                    Map.entry("confirmAccountNumber", "Confirm Account"),
                    Map.entry("ifscCode", "IFSC Code"),
                    Map.entry("bankName", "Bank Name")
            );
            case "entity-type" -> Map.of("entityType", "Entity Type");
            case "products" -> Map.of("products", "Products");
            case "doing-business" -> Map.of("operatingAddressDifferent", "Different Address");
            default -> Map.of();
        };
    }

    public void resetSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
