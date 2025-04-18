package miu.edu.cs516.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static software.amazon.awssdk.core.internal.waiters.ResponseOrException.response;

public class ProductHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final String tableName = "Products";
    private final Gson gson = new Gson();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String method = request.getHttpMethod();
        String productId = request.getPathParameters() != null ? request.getPathParameters().get("id") : null;
        String keyword = request.getQueryStringParameters() != null ? request.getQueryStringParameters().get("search") : null;

        switch (method) {
            case "GET":
                if (productId != null) {
                    return getProductById(productId);
                } else if (keyword != null) {
                    return searchProducts(keyword);
                } else {
                    return getAllProducts();
                }
            default:
                return response(405, "Method Not Allowed");
        }
    }

    // Get a product by its productId
    private APIGatewayProxyResponseEvent getProductById(String productId) {
        if (productId == null) return response(400, "Missing productId");

        // Query DynamoDB to get the product by productId
        var result = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("productId", AttributeValue.fromS(productId)))
                .build());

        if (!result.hasItem()) return response(404, "Product not found");

        // Convert result to product object
        Map<String, AttributeValue> item = result.item();
        Map<String, Object> product = Map.of(
                "productId", item.get("productId").s(),
                "name", item.get("name").s(),
                "price", Double.parseDouble(item.get("price").n()),
                "description", item.get("description").s(),
                "imageUrl", item.get("imageUrl").s()
        );
        return response(200, gson.toJson(product));
    }

    // Search products by name or description
    private APIGatewayProxyResponseEvent searchProducts(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return response(400, "Search term is required");
        }

        // Define a scan request to search by name or description
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("contains(#name, :keyword) OR contains(#description, :keyword)")
                .expressionAttributeNames(Map.of("#name", "name", "#description", "description"))
                .expressionAttributeValues(Map.of(":keyword", AttributeValue.fromS(keyword)))
                .build();

        // Execute the scan request
        ScanResponse result = dynamoDb.scan(scanRequest);

        List<Map<String, Object>> products = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.items()) {
            Map<String, Object> product = Map.of(
                    "productId", item.get("productId").s(),
                    "name", item.get("name").s(),
                    "price", Double.parseDouble(item.get("price").n()),
                    "description", item.get("description").s(),
                    "imageUrl", item.get("imageUrl").s()
            );
            products.add(product);
        }


        return response(200, gson.toJson(products));
    }

    // Get all products (with pagination support)
    private APIGatewayProxyResponseEvent getAllProducts() {
        ScanRequest.Builder scanRequestBuilder = ScanRequest.builder().tableName(tableName);

        ScanRequest scanRequest = scanRequestBuilder.build();

        // Execute the scan request
        ScanResponse result = dynamoDb.scan(scanRequest);

        List<Map<String, Object>> products = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.items()) {
            Map<String, Object> product = Map.of(
                    "productId", item.get("productId").s(),
                    "name", item.get("name").s(),
                    "price", Double.parseDouble(item.get("price").n()),
                    "description", item.get("description").s(),
                    "imageUrl", item.get("imageUrl").s()
            );
            products.add(product);
        }

        return response(200, gson.toJson(products));
    }

    private APIGatewayProxyResponseEvent response(int status, String body) {
        return new APIGatewayProxyResponseEvent().withStatusCode(status).withBody(body);
    }
}

