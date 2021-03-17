package servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.owlcs.ontapi.jena.model.OntIndividual;

import api.SearchResult;
import app.App;

@WebServlet("/query")
public class QueryServlet extends Servlet {
	private App app;

	@Override
	public void init() throws ServletException {
		app = new App();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		request.setCharacterEncoding("UTF-8");

//		String q = Utils.paramToUTF8(request.getParameter("q"));
		String q = request.getParameter("q");
		String t = request.getParameter("t");
		List<OntIndividual> individuals = new ArrayList<OntIndividual>();
		
		if (q != null) {
			// wiki search
			individuals = app.search(q);
		} else if (t != null) {
			// individuals by type
			individuals = app.getByType(t);
		}
		
		SearchResult result = new SearchResult(individuals);
		json(response, result.getResults());
	}
	
}
