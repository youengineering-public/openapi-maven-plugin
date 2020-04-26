/**
 * Copyright 2014 Reverb Technologies, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.carlkuesters.swagger.samples.jaxrs;

import com.sun.jersey.api.core.InjectParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/pet")
@Produces({"application/json", "application/xml"})
public class TestResource {

    @GET
    @Path("/{petId : [0-9]}")
    @Operation(
        summary = "Find pet by ID",
        description = "Returns a pet when ID < 10.  ID > 10 or nonintegers will simulate API error conditions",
        responses = {
            @ApiResponse(content = {@Content(schema = @Schema(implementation = Pet.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
            @ApiResponse(responseCode = "404", description = "Pet not found")
        }
    )
    public Response getPetById(
        @Parameter(description = "ID of pet that needs to be fetched", schema = @Schema(minimum = "1", maximum = "5"), required = true)
        @PathParam("petId") long petId
    ) {
        return Response.ok().entity(new Pet()).build();
    }

    @DELETE
    @Path("/{petId}")
    @Operation(
        summary = "Deletes a pet",
        responses = {
            @ApiResponse(responseCode = "400", description = "Invalid pet value"),
        }
    )
    public Response deletePet(
        @Parameter()
        @HeaderParam("api_key") String apiKey,

        @Parameter(description = "Pet id to delete", required = true)
        @PathParam("petId") Long petId
    ) {
        return Response.ok().build();
    }

    @POST
    @Consumes({"application/json", "application/xml"})
    @Operation(
        summary = "Add a new pet to the store",
        responses = {
            @ApiResponse(content = {@Content(schema = @Schema(implementation = Pet.class))}),
            @ApiResponse(responseCode = "405", description = "Invalid input")
        }
    )
    public Response addPet(
        @Parameter(description = "Pet object that needs to be added to the store", required = true) Pet pet
    ) {
        return Response.ok().entity(new Pet()).build();
    }

    @PUT
    @Consumes({"application/json", "application/xml"})
    @Operation(
        summary = "Update an existing pet",
        responses = {
            @ApiResponse(content = {@Content(schema = @Schema(implementation = Pet.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
            @ApiResponse(responseCode = "404", description = "Pet not found"),
            @ApiResponse(responseCode = "405", description = "Invalid input")
        }
    )
    public Response updatePet(
        @Parameter(description = "Pet object that needs to be added to the store", required = true) Pet pet
    ) {
        return Response.ok().entity(new Pet()).build();
    }

    @GET
    @Path("/pets/{petName : [^/]*}")
    @Operation(
        summary = "Finds Pets by name",
        responses = {
            @ApiResponse(content = {@Content(schema = @Schema(implementation = Pet.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid pet name")
        }
    )
    public Response findPetByPetName(
        @Parameter(description = "petName", required = true)
        @PathParam("petName") String petName
    ) {
        return Response.ok(new Pet()).build();
    }

    @GET
    @Path("/findByStatus")
    @Operation(
        summary = "Finds Pets by status",
        description = "Multiple status values can be provided with comma seperated strings",
        responses = {
            @ApiResponse(content = {@Content(schema = @Schema(implementation = Pet[].class))}),
            @ApiResponse(responseCode = "400", description = "Invalid status value")
        }
    )
    public Response findPetsByStatus(
        @Parameter(
            description = "Status values that need to be considered for filter",
            required = true,
            schema = @Schema(
                allowableValues = "available,pending,sold",
                defaultValue = "available"
            )
        )
        @QueryParam("status") String status
    ) {
        return Response.ok(new Pet[]{ new Pet(), new Pet() }).build();
    }

    @GET
    @Path("/findByTags")
    @Operation(
        summary = "Finds Pets by tags",
        description =  "Muliple tags can be provided with comma seperated strings. Use tag1, tag2, tag3 for testing.",
        responses = {
                @ApiResponse(content = {@Content(schema = @Schema(implementation = Pet[].class))}),
                @ApiResponse(responseCode = "400", description = "Invalid tag value")
        }
    )
    @Deprecated
    public Response findPetsByTags(
        @Parameter(description = "Tags to filter by", required = true)
        @QueryParam("tags") String tags
    ) {
        return Response.ok(new Pet[]{ new Pet(), new Pet() }).build();
    }

	@GET
	@Path("/findAll")
    @Operation(
        summary = "Finds all Pets",
        description =  "Returns a paginated list of all the Pets.",
        responses = {
                @ApiResponse(content = {@Content(schema = @Schema(implementation = Pet[].class))}),
                @ApiResponse(responseCode = "400", description = "Invalid page number value")
        }
    )
	public Response findAllPaginated(
        @Parameter(description = "pageNumber", required = true)
        @QueryParam("pageNumber") int pageNumber
    ) {
        return Response.ok(new Pet[]{ new Pet(), new Pet() }).build();
	}

    @POST
    @Path("/{petId}")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Operation(
        summary = "Updates a pet in the store with form data",
        responses = {
                @ApiResponse(responseCode = "405", description = "Invalid input")
        }
    )
    public Response updatePetWithForm(
        @BeanParam MyBean myBean
    ) {
        return Response.ok().build();
    }

    @POST
    @Path("/{petId}/testInjectParam")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Operation(
        summary = "Updates a pet in the store with form data",
        responses = {
                @ApiResponse(responseCode = "405", description = "Invalid input")
        }
    )
    public Response updatePetWithFormViaInjectParam(
        @InjectParam MyBean myBean
    ) {
        return Response.ok().build();
    }

    @GET
    @Produces("application/json")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Operation(summary = "Returns pet")
    public Pet get(
        @Parameter(hidden = true, name = "hiddenParameter")
        @QueryParam("hiddenParameter") String hiddenParameter
    ) {
        return new Pet();
    }

    @GET
    @Path("/test")
    @Produces("application/json")
    @Operation(summary = "Returns pet")
    public Pet test(
        @Parameter(description = "Test pet as json string in query")
        @QueryParam("pet") Pet pet
    ) {
        return new Pet();
    }

    @GET
    @Path("/test/extensions")
    @Operation(
        summary = "testExtensions",
        extensions = {
            @Extension(name = "firstExtension", properties = {
                    @ExtensionProperty(name = "extensionName1", value = "extensionValue1"),
                    @ExtensionProperty(name = "extensionName2", value = "extensionValue2")
            }),
            @Extension(properties = {
                    @ExtensionProperty(name = "extensionName3", value = "extensionValue3")})
        }
    )
    public String testingExtensions() {
        return "Blubb";
    }

    @GET
    @Produces("application/json")
    @Operation(summary = "testingHiddenApiOperation", hidden = true)
    public String testingHiddenApiOperation() {
        return "testingHiddenApiOperation";
    }

    @GET
    @Path("/test/testingArrayResponse")
    @Operation(
            summary = "testingArrayResponse",
            responses = {@ApiResponse(content = {@Content(schema = @Schema(implementation = Pet[].class))})}
    )
    public Response testingArrayResponse() {
        return Response.ok(new Pet[]{ new Pet(), new Pet() }).build();
    }

    @GET
    @Path("/test/testingVendorExtensions")
    @Operation(summary = "testingVendorExtensions")
    @TestVendorExtension.TestVendorAnnotation
    public Response testingVendorExtensions() {
        return null;
    }
}
