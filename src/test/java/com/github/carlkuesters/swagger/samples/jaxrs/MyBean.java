package com.github.carlkuesters.swagger.samples.jaxrs;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.*;
import java.util.List;

@Getter
@Setter
public class MyBean extends MyParentBean {

    @Parameter(description = "ID of pet that needs to be updated", required = true)
    @PathParam("petId")
    private String petId;

    @Parameter(description = "Updated name of the pet", schema = @Schema(defaultValue = "defaultValue"))
    @FormParam("name")
    private String name;

    @Parameter(description = "Updated status of the pet", schema = @Schema(allowableValues = "value1, value2"))
    @FormParam("status")
    private String status;

    @HeaderParam("myHeader")
    private String myHeader;

    @HeaderParam("intValue")
    private int intValue;

    @Parameter(description = "hidden", hidden = true)
    @QueryParam(value = "hiddenValue")
    private String hiddenValue;

    @QueryParam(value = "listValue")
    private List<String> listValue;
    
    @BeanParam
    private MyNestedBean nestedBean;
    
    /**
     * This field is to test that bean params using constructor injection behave
     * correctly. It's also nested just to avoid adding too much test code.
     */
    @BeanParam
    private MyConstructorInjectedNestedBean constructorInjectedNestedBean;

    @Parameter(description = "testIntegerAllowableValues", schema = @Schema(allowableValues = "25, 50, 100", defaultValue = "25"))
    @QueryParam("testIntegerAllowableValues")
    public Integer testIntegerAllowableValues;
    
    /**
     * This field's allowableValues, required, pattern, and defaultValue should
     * be derived based on its JAX-RS and validation annotations.
     */
    @QueryParam("constrainedField")
    @Min(25L)
    @Max(75L)
    @NotNull
    @Pattern(regexp = "[0-9]5")
    @DefaultValue("55")
    private int constrainedField;

}
