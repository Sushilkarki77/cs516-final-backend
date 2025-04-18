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
            Map<String, Object> claims = (Map<String, Object>) ((Map<String, Object>) request.getRequestContext().getAuthorizer()).get("claims");
            return (String) claims.get("sub");
        } catch (Exception e) {
            return null;
        }
    }

    private APIGatewayProxyResponseEvent createOrder(String userId, String body) {
        try {
            Map<String, Object> data = gson.fromJson(body, Map.class);
            String orderId = UUID.randomUUID().toString();
            double totalPrice = ((Double) data.get("totalPrice"));
            String shippingAddress = (String) data.get("shippingAddress");

            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
            List<AttributeValue> itemList = new ArrayList<>();
            for (Map<String, Object> item : items) {
                Map<String, AttributeValue> itemMap = new HashMap<>();
                itemMap.put("productId", AttributeValue.fromS((String) item.get("productId")));
                itemMap.put("quantity", AttributeValue.fromN(String.valueOf(((Double) item.get("quantity")).intValue())));
                itemMap.put("price", AttributeValue.fromN(String.valueOf((Double) item.get("price"))));
                itemList.add(AttributeValue.fromM(itemMap));
            }

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("orderId", AttributeValue.fromS(orderId));
            item.put("userId", AttributeValue.fromS(userId));
            item.put("totalPrice", AttributeValue.fromN(String.valueOf(totalPrice)));
            item.put("shippingAddress", AttributeValue.fromS(shippingAddress));
            item.put("createdAt", AttributeValue.fromS(new Date().toString()));
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
                order.put("shippingAddress", item.get("shippingAddress").s());
                order.put("createdAt", item.get("createdAt").s());

                // Deserialize items array
                List<AttributeValue> itemsAttr = item.get("items").l();
                List<Map<String, Object>> itemList = new ArrayList<>();
                for (AttributeValue i : itemsAttr) {
                    Map<String, AttributeValue> m = i.m();
                    itemList.add(Map.of(
                            "productId", m.get("productId").s(),
                            "quantity", Integer.parseInt(m.get("quantity").n()),
                            "price", Double.parseDouble(m.get("price").n())
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
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }
}

