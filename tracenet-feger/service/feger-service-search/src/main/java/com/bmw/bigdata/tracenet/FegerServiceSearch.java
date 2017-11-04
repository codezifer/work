package com.bmw.bigdata.tracenet;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bmw.bigdata.tracenet.dto.SearchRequestDTO;
import com.bmw.bigdata.tracenet.dto.SearchResponseDTO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api("search")
@RequestScoped
@Path("/search")
public class FegerServiceSearch {

	@ApiOperation(value = "Search traces by request", notes = "Search traces by request", response = SearchResponseDTO.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Search request was triggered") })
	@POST
	@Path("/request")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResponseDTO search(SearchRequestDTO searchRequest) {
		SearchResponseDTO response = new SearchResponseDTO();
		return response;
	}
}
