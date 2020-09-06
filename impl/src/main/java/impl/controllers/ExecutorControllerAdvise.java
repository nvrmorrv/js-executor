package impl.controllers;

import impl.service.exceptions.DeletionException;
import impl.service.exceptions.ExecTimeOutException;
import impl.service.exceptions.SyntaxErrorException;
import impl.service.exceptions.UnknownIdException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import rest.api.dto.ErrorResp;
import rest.api.dto.SyntaxErrorResp;
import rest.api.dto.TimeoutErrorResp;

@ControllerAdvice(basePackageClasses = ExecutorController.class)
@ResponseBody
@Hidden
@Slf4j
public class ExecutorControllerAdvise {

  @ExceptionHandler(SyntaxErrorException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public SyntaxErrorResp response(SyntaxErrorException ex) {
    return new SyntaxErrorResp(
          ex.getMessage(),
          ex.getDesc(),
          ex.getSection()
    );
  }

  @ExceptionHandler(ExecTimeOutException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public TimeoutErrorResp response(ExecTimeOutException ex) {
    return new TimeoutErrorResp(
          ex.getMessage(),
          ex.getOutput()
    );
  }

  @ExceptionHandler(UnknownIdException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResp response(UnknownIdException ex) {
    return new ErrorResp(ex.getMessage());
  }

  @ExceptionHandler(DeletionException.class)
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public ErrorResp response(DeletionException ex) {
    return new ErrorResp(ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ErrorResp response(Exception ex) {
    log.error("ERROR: class: {}, message: {}, stack trace: {}",
          ex.getClass().getCanonicalName(), ex.getMessage(), ex.getStackTrace());
    return new ErrorResp("Internal server error");
  }
}
