package servlet.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorInfo {
    private String code;
    private String message;
    private Object details;

    public ErrorInfo() {}

    public ErrorInfo(String code, String message, Object details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Object getDetails() { return details; }
    public void setDetails(Object details) { this.details = details; }
}
