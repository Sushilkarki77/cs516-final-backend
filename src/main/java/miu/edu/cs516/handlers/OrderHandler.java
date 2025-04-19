package miu.edu.cs516.handlers;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import com.google.gson.*;

import java.util.*;

public class OrderHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final String tableName = "Orders";
    private final Gson gson = new Gson();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String method = request.getHttpMethod();
        System.out.println("RequestContext: " + gson.toJson(request.getRequestContext()));

        String userId = extractUserId(request);
        if (userId == null) return response(401, "Unauthorized");

        if ("POST".equalsIgnoreCase(method)) {
            return createOrder(userId, request.getBody());
        } else if ("GET".equalsIgnoreCase(method)) {
            return getOrdersByUserId(userId);
        } else {
            return response(405, "Method Not Allowed");
        }
    }

    private String extractUserId(APIGatewayProxyRequestEvent request) {
        try {
            // Log the request context and authorizer claims for debugging
            System.out.println("REQUEST CONTEXT: " + new Gson().toJson(request.getRequestContext()));


            Map<String, Object> authorizer = (Map<String, Object>) request.getRequestContext().getAuthorizer();
            if (authorizer == null) return null;

            Map<String, Object> claims = (Map<String, Object>) authorizer.get("claims");
            if (claims == null) return null;

            return (String) claims.get("sub");
        } catch (Exception e) {
            e.printStackTrace(); // optional for debugging
            return null;
        }
    }

    private APIGatewayProxyResponseEvent createOrder(String userId, String body) {
        try {
            Map<String, Object> data = gson.fromJson(body, Map.class);
            String orderId = UUID.randomUUID().toString();
            double totalPrice = ((Double) data.get("totalPrice"));

            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
            List<AttributeValue> itemList = new ArrayList<>();
            for (Map<String, Object> item : items) {
                String productId = String.valueOf(item.get("productId"));
                String imageUrl = String.valueOf(item.get("imageUrl"));
                String name = String.valueOf(item.get("name"));
                int quantity = ((Number) item.get("quantity")).intValue();
                double price = ((Number) item.get("price")).doubleValue();

                Map<String, AttributeValue> itemMap = new HashMap<>();
                itemMap.put("productId", AttributeValue.fromS(productId));
                itemMap.put("quantity", AttributeValue.fromN(String.valueOf(quantity)));
                itemMap.put("price", AttributeValue.fromN(String.valueOf(price)));
                itemMap.put("name", AttributeValue.fromS(name));
                itemMap.put("imageUrl", AttributeValue.fromS(imageUrl));

                itemList.add(AttributeValue.fromM(itemMap));
            }


            Map<String, AttributeValue> item = new HashMap<>();
            item.put("orderId", AttributeValue.fromS(orderId));
            item.put("userId", AttributeValue.fromS(userId));
            item.put("totalPrice", AttributeValue.fromN(String.valueOf(totalPrice)));
            item.put("createdAt", AttributeValue.fromS(new Date().toString()));
            item.put("orderStatus", AttributeValue.fromS("Confirmed"));
            item.put("items", AttributeValue.fromL(itemList));

            dynamoDb.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build());

            return response(201, gson.toJson(Map.of("message", "Order created", "orderId", orderId)));

        } catch (Exception e) {
            return response(500, "Error creating order: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent getOrdersByUserId(String userId) {
        try {
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("UserIdIndex") // Ensure a GSI exists on userId
                    .keyConditionExpression("userId = :uid")
                    .expressionAttributeValues(Map.of(
                            ":uid", AttributeValue.fromS(userId)
                    ))
                    .build();

            QueryResponse queryResponse = dynamoDb.query(queryRequest);

            List<Map<String, AttributeValue>> items = queryResponse.items();
            List<Map<String, Object>> orders = new ArrayList<>();

            for (Map<String, AttributeValue> item : items) {
                Map<String, Object> order = new HashMap<>();
                order.put("orderId", item.get("orderId").s());
                order.put("userId", item.get("userId").s());
                order.put("totalPrice", Double.parseDouble(item.get("totalPrice").n()));
                order.put("createdAt", item.get("createdAt").s());
                order.put("orderStatus", item.get("orderStatus").s());


                // Deserialize items array
                List<AttributeValue> itemsAttr = item.get("items").l();
                List<Map<String, Object>> itemList = new ArrayList<>();
                for (AttributeValue i : itemsAttr) {
                    Map<String, AttributeValue> m = i.m();
                    itemList.add(Map.of(
                            "productId", m.get("productId").s(),
                            "quantity", Integer.parseInt(m.get("quantity").n()),
                            "price", Double.parseDouble(m.get("price").n()),
                            "imageUrl", m.get("imageUrl").s(),
                            "name", m.get("name").s()


                    ));
                }
                order.put("items", itemList);

                orders.add(order);
            }

            return response(200, gson.toJson(orders));

        } catch (Exception e) {
            return response(500, "Error retrieving orders: " + e.getMessage());
        }
    }


    private APIGatewayProxyResponseEvent response(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", "application/json",
                        "Access-Control-Allow-Origin", "*",
                        "Access-Control-Allow-Methods", "GET,POST,OPTIONS",
                        "Access-Control-Allow-Headers", "*"))
                .withBody(body);
    }
}

