package com.bmw.bigdata.tracenet;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bmw.bigdata.tracenet.dto.SearchRequestDTO;
import com.bmw.bigdata.tracenet.dto.SearchResponseDTO;

@RequestScoped
@Path("/service")
public class FegerService {

	@POST
	@Path("/request")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResponseDTO search(SearchRequestDTO searchRequest) {
		SearchResponseDTO response = new SearchResponseDTO();
		return response;
	}
}
