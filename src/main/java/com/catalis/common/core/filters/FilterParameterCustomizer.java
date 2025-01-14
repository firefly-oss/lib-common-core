package com.catalis.common.core.filters;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springdoc.core.annotations.ParameterObject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FilterParameterCustomizer is responsible for customizing OpenAPI/Swagger documentation
 * for endpoints that use FilterRequest parameters. It automatically generates documentation
 * for filter fields, pagination parameters, and range filters while properly handling
 * special cases like ID fields.
 *
 * The customization process follows these steps:
 * 1. Preserves existing path parameters from the operation
 * 2. Identifies endpoints using FilterRequest parameters with @ParameterObject or @ModelAttribute
 * 3. Extracts the DTO class used for filtering
 * 4. Adds standard pagination parameters
 * 5. Processes DTO fields to create filter parameters:
 *    - Skips ID fields (fields with @Id annotation or ending with "Id")
 *    - Creates basic filter parameters for regular fields
 *    - Creates range parameters (from/to) for numeric and date fields
 * 6. Combines preserved parameters with new filter parameters
 *
 * Features:
 * - Preserves PathVariable parameters in documentation
 * - Automatic detection and handling of ID fields
 * - Support for range-based filtering on numeric and date fields
 * - Pagination parameter documentation
 * - Sorting parameter documentation
 * - Camel case to words conversion for readable descriptions
 */
@Component
public class FilterParameterCustomizer implements OperationCustomizer {

    /**
     * Customizes the OpenAPI operation by adding filter parameters while preserving path variables.
     * This is called by SpringDoc for each endpoint that uses FilterRequest.
     *
     * @param operation The OpenAPI operation to customize
     * @param handlerMethod The Spring handler method being documented
     * @return The customized operation
     */
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        // Get existing parameters and preserve path parameters
        List<Parameter> existingParameters = operation.getParameters() != null ?
                operation.getParameters() : new ArrayList<>();

        // Keep path parameters and other non-query parameters
        List<Parameter> preservedParameters = existingParameters.stream()
                .filter(param -> "path".equals(param.getIn()) || !"query".equals(param.getIn()))
                .collect(Collectors.toList());

        // Process only FilterRequest parameters with @ParameterObject or @ModelAttribute
        Arrays.stream(handlerMethod.getMethodParameters())
                .filter(param -> FilterRequest.class.isAssignableFrom(param.getParameterType()) &&
                        (param.hasParameterAnnotation(ParameterObject.class) ||
                                param.hasParameterAnnotation(ModelAttribute.class)))
                .findFirst()
                .ifPresent(param -> {
                    Class<?> dtoClass = extractDtoClass(param.getGenericParameterType());
                    if (dtoClass != null) {
                        List<Parameter> filterParameters = new ArrayList<>(preservedParameters);
                        addFilterParameters(filterParameters, dtoClass);
                        operation.setParameters(filterParameters);
                    }
                });

        return operation;
    }

    /**
     * Extracts the DTO class from a parameterized FilterRequest type.
     * For example, from FilterRequest<UserDTO> it extracts UserDTO.class.
     *
     * @param type The generic parameter type
     * @return The class of the DTO used in the FilterRequest
     */
    private Class<?> extractDtoClass(java.lang.reflect.Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            return (Class<?>) paramType.getActualTypeArguments()[0];
        }
        return null;
    }

    /**
     * Adds all necessary filter parameters to the parameter list.
     * This includes pagination parameters and parameters derived from the DTO fields.
     *
     * @param parameters The list of parameters to add to
     * @param dtoClass The DTO class to extract fields from
     */
    private void addFilterParameters(List<Parameter> parameters, Class<?> dtoClass) {
        // Add standard pagination parameters
        parameters.add(createParameter("pageNumber", "integer", "Page number (0-based)", "0"));
        parameters.add(createParameter("pageSize", "integer", "Number of items per page", "10"));
        parameters.add(createParameter("sortBy", "string", "Field to sort by", null));
        parameters.add(createParameter("sortDirection", "string", "Sort direction (ASC or DESC)", "DESC"));

        // Process each field in the DTO class
        for (Field field : dtoClass.getDeclaredFields()) {
            if (shouldIncludeField(field) && !isIdField(field)) {
                parameters.add(createParameterFromField(field));

                if (isRangeableField(field)) {
                    parameters.add(createRangeStartParameter(field));
                    parameters.add(createRangeEndParameter(field));
                }
            }
        }
    }

    /**
     * Determines if a field should be included in the filter parameters.
     * Excludes static, transient, and specific system fields.
     *
     * @param field The field to check
     * @return true if the field should be included
     */
    private boolean shouldIncludeField(Field field) {
        int modifiers = field.getModifiers();
        return !java.lang.reflect.Modifier.isStatic(modifiers)
                && !java.lang.reflect.Modifier.isTransient(modifiers)
                && !field.getName().equals("serialVersionUID");
    }

    /**
     * Checks if a field is an ID field, either by annotation or naming convention.
     * This is used to exclude ID fields from filtering.
     *
     * @param field The field to check
     * @return true if the field is an ID field
     */
    private boolean isIdField(Field field) {
        return field.isAnnotationPresent(Id.class)
                || field.getName().endsWith("Id")
                || field.getName().equals("id");
    }

    /**
     * Determines if a field should support range-based filtering.
     * Applies to numeric and date/time fields, but not to ID fields.
     *
     * @param field The field to check
     * @return true if the field should support range filtering
     */
    private boolean isRangeableField(Field field) {
        if (isIdField(field)) {
            return false;
        }
        Class<?> type = field.getType();
        return Number.class.isAssignableFrom(type)
                || type.equals(LocalDateTime.class)
                || type.equals(java.util.Date.class);
    }

    /**
     * Creates a basic OpenAPI parameter with the specified properties.
     *
     * @param name Parameter name
     * @param type Parameter type
     * @param description Parameter description
     * @param defaultValue Default value (can be null)
     * @return Created Parameter object
     */
    private Parameter createParameter(String name, String type, String description, String defaultValue) {
        Schema<?> schema = new Schema<>().type(type);
        if (defaultValue != null) {
            schema.setDefault(defaultValue);
        }

        return new Parameter()
                .name(name)
                .in("query")
                .description(description)
                .required(false)
                .schema(schema);
    }

    /**
     * Creates a filter parameter for a specific field.
     *
     * @param field The field to create a parameter for
     * @return Created Parameter object
     */
    private Parameter createParameterFromField(Field field) {
        return createParameter(
                field.getName(),
                getOpenApiType(field.getType()),
                "Filter by " + camelCaseToWords(field.getName()),
                null
        );
    }

    /**
     * Creates a "from" range parameter for a field.
     *
     * @param field The field to create a range parameter for
     * @return Created Parameter object
     */
    private Parameter createRangeStartParameter(Field field) {
        return createParameter(
                field.getName() + "From",
                getOpenApiType(field.getType()),
                "Filter " + camelCaseToWords(field.getName()) + " from value",
                null
        );
    }

    /**
     * Creates a "to" range parameter for a field.
     *
     * @param field The field to create a range parameter for
     * @return Created Parameter object
     */
    private Parameter createRangeEndParameter(Field field) {
        return createParameter(
                field.getName() + "To",
                getOpenApiType(field.getType()),
                "Filter " + camelCaseToWords(field.getName()) + " to value",
                null
        );
    }

    /**
     * Maps Java types to OpenAPI type strings.
     *
     * @param type The Java type to map
     * @return The corresponding OpenAPI type string
     */
    private String getOpenApiType(Class<?> type) {
        if (type.isEnum()) return "string";
        if (type == String.class) return "string";
        if (type == Integer.class || type == int.class) return "integer";
        if (type == Long.class || type == long.class) return "integer";
        if (type == Double.class || type == double.class) return "number";
        if (type == Boolean.class || type == boolean.class) return "boolean";
        if (type == LocalDateTime.class) return "string";
        return "string";
    }

    /**
     * Converts camelCase strings to space-separated words.
     * Example: "firstName" becomes "first name"
     *
     * @param camelCase The camelCase string to convert
     * @return The converted string with spaces
     */
    private String camelCaseToWords(String camelCase) {
        String[] words = camelCase.split("(?=[A-Z])");
        return String.join(" ", words).toLowerCase();
    }
}