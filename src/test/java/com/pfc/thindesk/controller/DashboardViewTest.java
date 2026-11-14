package com.pfc.thindesk.controller;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import static org.junit.jupiter.api.Assertions.*;

class DashboardViewTest {
    @Test
    void renderizaDashboardComSidebarEUrlsDosRecursos() throws Exception {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var servletContext = new MockServletContext();
        var request = new MockHttpServletRequest(servletContext);
        request.setRequestURI("/dashboard");
        var response = new MockHttpServletResponse();
        var application = JakartaServletWebApplication.buildApplication(servletContext);
        var context = new WebContext(application.buildExchange(request, response));
        var html = engine.process("dashboard", context);
        assertTrue(html.contains("data-api-base=\"/api/dashboard\""));
        assertTrue(html.contains("src=\"/js/dashboard.js\""));
        assertTrue(html.contains("href=\"/css/dashboard.css\""));
        assertTrue(html.contains("id=\"sidebar\""));
        assertFalse(html.contains("th:replace"));
        Files.writeString(Path.of("target/dashboard-preview.html"), html);
    }

    @Test
    void paginaInicialEDashboardUsamNovaView() {
        assertEquals("dashboard", new HomeController().home().getViewName());
    }
}
