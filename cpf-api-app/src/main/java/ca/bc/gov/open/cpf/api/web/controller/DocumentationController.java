package ca.bc.gov.open.cpf.api.web.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.HandlerMapping;

import com.revolsys.util.Property;

@Controller
public class DocumentationController extends WebApplicationObjectSupport {

  protected MediaType getMediaType(final Resource resource) {
    final String mimeType = getServletContext().getMimeType(
      resource.getFilename());
    if (Property.hasValue(mimeType)) {
      return MediaType.parseMediaType(mimeType);
    } else {
      return null;
    }
  }

  @RequestMapping(value = {
    "/docs/**", "/secure/docs/**"
  })
  public void handleRequest(final HttpServletRequest request,
    final HttpServletResponse response) throws ServletException, IOException {
    String path = (String)request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    if (path == null) {
      throw new IllegalStateException("Required request attribute '"
        + HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
    } else {
      path = StringUtils.cleanPath(path);
      if (!Property.hasValue(path)) {
        path = "index.html";
      }
      if (!isInvalidPath(path)) {
        final ServletContextResource docsResource = new ServletContextResource(
          getServletContext(), "/docs/");
        Resource resource = docsResource.createRelative(path);
        if (resource.exists() && resource.isReadable()) {
          MediaType mediaType = getMediaType(resource);
          if (mediaType == null) {
            resource = docsResource.createRelative(path + "/index.html");
            mediaType = getMediaType(resource);
          }

          try {
            final InputStream inputStream = resource.getInputStream();
            setHeaders(response, resource, mediaType);
            FileCopyUtils.copy(inputStream, response.getOutputStream());
          } catch (final FileNotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
          }
        } else {
          response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
      }
    }

  }

  protected boolean isInvalidPath(final String path) {
    return path.contains("WEB-INF") || path.contains("META-INF")
        || path.startsWith("..");
  }

  protected void setHeaders(final HttpServletResponse response,
    final Resource resource, final MediaType mediaType) throws IOException {
    final long length = resource.contentLength();
    if (length > Integer.MAX_VALUE) {
      throw new IOException(
        "Resource content too long (beyond Integer.MAX_VALUE): " + resource);
    }
    response.setContentLength((int)length);

    if (mediaType != null) {
      response.setContentType(mediaType.toString());
    }
  }

}
