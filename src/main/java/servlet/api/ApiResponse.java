package servlet.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String status; // "success" ou "error"
    private Integer code;  // Code HTTP (200, 404, 500...)
    private T data;
    private ErrorInfo error;

    public ApiResponse() {}

    // Méthode factory pour success
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.status = "success";
        res.code = 200;
        res.data = data;
        return res;
    }

    // Méthode factory pour error
    public static <T> ApiResponse<T> error(int code, String message, Object details) {
        ApiResponse<T> res = new ApiResponse<>();
        res.status = "error";
        res.code = code;
        res.error = new ErrorInfo(String.valueOf(code), message, details);
        return res;
    }

    // Getters et Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    
    public ErrorInfo getError() { return error; }
    public void setError(ErrorInfo error) { this.error = error; }
}