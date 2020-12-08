package servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void json(HttpServletResponse response, Object data) throws IOException {
		String json = new Gson().toJson(data);
		
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");
		
		PrintWriter out = response.getWriter();
		out.print(json);
		out.flush();
	}
	
	protected void html(HttpServletResponse response, String html) throws IOException {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		
		PrintWriter out = response.getWriter();
		out.print(html);
		out.flush();
	}
}
