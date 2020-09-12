package impl.controllers;

import impl.service.exceptions.DeletionException;
import impl.service.exceptions.SyntaxErrorException;
import impl.service.exceptions.UnknownIdException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

@ControllerAdvice(basePackageClasses = ExecutorController.class)
@ResponseBody
@Hidden
@Slf4j
public class ExecutorControllerAdvise {

  @ExceptionHandler(SyntaxErrorException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Problem response(SyntaxErrorException ex) {
    return Problem.builder()
     .withTitle(ex.getMessage())
     .withStatus(Status.BAD_REQUEST)
     .withDetail(ex.getDesc())
     .with("section", ex.getSection())
     .build();
  }


  @ExceptionHandler(UnknownIdException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Problem response(UnknownIdException ex) {
    return Problem.builder()
     .withTitle("Unknown id")
     .withStatus(Status.NOT_FOUND)
     .withDetail(ex.getMessage())
     .build();
  }

  @ExceptionHandler(DeletionException.class)
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public Problem response(DeletionException ex) {
    return Problem.builder()
     .withTitle("Attempt to delete running script")
     .withStatus(Status.METHOD_NOT_ALLOWED)
     .withDetail(ex.getMessage())
     .build();
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Problem response(Exception ex) {
    log.error("ERROR: class: {}, message: {}, stack trace: {}",
          ex.getClass().getCanonicalName(), ex.getMessage(), ex.getStackTrace());
    return Problem.valueOf(Status.INTERNAL_SERVER_ERROR);
  }
}
