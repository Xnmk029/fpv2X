package com.iung.fpv20.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DroneModelRenderer {
    private static final Gson GSON = new Gson();

    public static class ModelCube {
        public String name;
        public float[] from;
        public float[] to;
        public float[] rotation; // [x, y, z] in degrees
        public float[] origin; // [x, y, z] pivot
        public float[] uvUp = {0, 0, 16, 16};
        public float[] uvDown = {0, 0, 16, 16};
        public float[] uvNorth = {0, 0, 16, 16};
        public float[] uvSouth = {0, 0, 16, 16};
        public float[] uvWest = {0, 0, 16, 16};
        public float[] uvEast = {0, 0, 16, 16};

        public int texUp = 0;
        public int texDown = 0;
        public int texNorth = 0;
        public int texSouth = 0;
        public int texWest = 0;
        public int texEast = 0;
    }

    public static class DroneModel {
        public final List<ModelCube> cubes = new ArrayList<>();
        public final java.util.Map<Integer, Identifier> textureMap = new java.util.HashMap<>();

        public DroneModel(Identifier modelId) {
            try {
                Optional<Resource> resourceOpt = MinecraftClient.getInstance().getResourceManager().getResource(modelId);
                if (resourceOpt.isPresent()) {
                    try (InputStreamReader reader = new InputStreamReader(resourceOpt.get().getInputStream(), StandardCharsets.UTF_8)) {
                        JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                        
                        if (obj.has("textures")) {
                            JsonObject texObj = obj.getAsJsonObject("textures");
                            for (java.util.Map.Entry<String, JsonElement> entry : texObj.entrySet()) {
                                try {
                                    int key = Integer.parseInt(entry.getKey());
                                    String val = entry.getValue().getAsString();
                                    String[] split = val.split(":");
                                    String namespace = split[0];
                                    String path = "textures/" + split[1] + ".png";
                                    textureMap.put(key, new Identifier(namespace, path));
                                } catch (Exception e) {
                                    // ignore invalid textures
                                }
                            }
                        }

                        if (obj.has("elements")) {
                            JsonArray elements = obj.getAsJsonArray("elements");
                            for (JsonElement element : elements) {
                                JsonObject elemObj = element.getAsJsonObject();
                                ModelCube cube = new ModelCube();
                                cube.name = elemObj.has("name") ? elemObj.get("name").getAsString() : "";
                                
                                JsonArray fromArr = elemObj.getAsJsonArray("from");
                                cube.from = new float[]{
                                        fromArr.get(0).getAsFloat(),
                                        fromArr.get(1).getAsFloat(),
                                        fromArr.get(2).getAsFloat()
                                };

                                JsonArray toArr = elemObj.getAsJsonArray("to");
                                cube.to = new float[]{
                                        toArr.get(0).getAsFloat(),
                                        toArr.get(1).getAsFloat(),
                                        toArr.get(2).getAsFloat()
                                };

                                if (elemObj.has("rotation")) {
                                    JsonObject rotObj = elemObj.getAsJsonObject("rotation");
                                    cube.rotation = new float[3];
                                    if (rotObj.has("angle") && rotObj.has("axis")) {
                                        float angle = rotObj.get("angle").getAsFloat();
                                        String axis = rotObj.get("axis").getAsString();
                                        if (axis.equalsIgnoreCase("x")) {
                                            cube.rotation[0] = angle;
                                        } else if (axis.equalsIgnoreCase("y")) {
                                            cube.rotation[1] = angle;
                                        } else if (axis.equalsIgnoreCase("z")) {
                                            cube.rotation[2] = angle;
                                        }
                                    } else {
                                        cube.rotation[0] = rotObj.has("x") ? rotObj.get("x").getAsFloat() : 0f;
                                        cube.rotation[1] = rotObj.has("y") ? rotObj.get("y").getAsFloat() : 0f;
                                        cube.rotation[2] = rotObj.has("z") ? rotObj.get("z").getAsFloat() : 0f;
                                    }
                                    JsonArray origArr = rotObj.getAsJsonArray("origin");
                                    cube.origin = new float[]{
                                            origArr.get(0).getAsFloat(),
                                            origArr.get(1).getAsFloat(),
                                            origArr.get(2).getAsFloat()
                                    };
                                }

                                if (elemObj.has("faces")) {
                                    JsonObject facesObj = elemObj.getAsJsonObject("faces");
                                    cube.uvUp = parseFaceUv(facesObj, "up");
                                    cube.texUp = parseFaceTexture(facesObj, "up");
                                    cube.uvDown = parseFaceUv(facesObj, "down");
                                    cube.texDown = parseFaceTexture(facesObj, "down");
                                    cube.uvNorth = parseFaceUv(facesObj, "north");
                                    cube.texNorth = parseFaceTexture(facesObj, "north");
                                    cube.uvSouth = parseFaceUv(facesObj, "south");
                                    cube.texSouth = parseFaceTexture(facesObj, "south");
                                    cube.uvWest = parseFaceUv(facesObj, "west");
                                    cube.texWest = parseFaceTexture(facesObj, "west");
                                    cube.uvEast = parseFaceUv(facesObj, "east");
                                    cube.texEast = parseFaceTexture(facesObj, "east");
                                }
                                cubes.add(cube);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private int parseFaceTexture(JsonObject facesObj, String faceName) {
            if (facesObj.has(faceName)) {
                JsonObject faceObj = facesObj.getAsJsonObject(faceName);
                if (faceObj.has("texture")) {
                    String texStr = faceObj.get("texture").getAsString();
                    if (texStr.startsWith("#")) {
                        try {
                            return Integer.parseInt(texStr.substring(1));
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    }
                }
            }
            return 0;
        }

        private float[] parseFaceUv(JsonObject facesObj, String faceName) {
            if (facesObj.has(faceName)) {
                JsonObject faceObj = facesObj.getAsJsonObject(faceName);
                if (faceObj.has("uv")) {
                    JsonArray uvArr = faceObj.getAsJsonArray("uv");
                    return new float[]{
                            uvArr.get(0).getAsFloat(),
                            uvArr.get(1).getAsFloat(),
                            uvArr.get(2).getAsFloat(),
                            uvArr.get(3).getAsFloat()
                    };
                }
            }
            return new float[]{0, 0, 16, 16};
        }
    }

    private static DroneModel frameModel;
    private static DroneModel motorModel;
    private static DroneModel propellerModel;
    private static DroneModel batteryModel;

    public static void init() {
        frameModel = new DroneModel(new Identifier("fpv20", "models/drone/frame.json"));
        motorModel = new DroneModel(new Identifier("fpv20", "models/drone/motor.json"));
        propellerModel = new DroneModel(new Identifier("fpv20", "models/drone/propeller.json"));
        batteryModel = new DroneModel(new Identifier("fpv20", "models/drone/battery.json"));
    }

    public static void renderDrone(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay,
                                    float propRotationAngle) {
        // Fallback to standard 5" build config
        renderDrone(matrices, vertexConsumers, light, overlay, 2, 2, 3, 0, 5.0f, 4.0f, propRotationAngle);
    }

    public static void renderDrone(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay,
                                    int frameIndex, int motorIndex, int batteryIndex, int cameraIndex,
                                    float propDiameter, float propPitch, float propRotationAngle) {
        if (frameModel == null) {
            init();
        }

        matrices.push();
        // Scale down to match normal block scale
        matrices.scale(0.03125f, 0.03125f, 0.03125f);

        // Frame configuration mapping
        float frameScale = 1.0f;
        float motorDistance = 8.5f;
        switch (frameIndex) {
            case 0: // 2" Whoop
                frameScale = 0.5f;
                motorDistance = 4.2f;
                break;
            case 1: // 3" Cinewhoop
                frameScale = 0.72f;
                motorDistance = 6.0f;
                break;
            case 2: // 5" Freestyle
                frameScale = 1.0f;
                motorDistance = 8.5f;
                break;
            case 3: // 7" LongRange
                frameScale = 1.35f;
                motorDistance = 11.5f;
                break;
        }

        // 1. Draw Frame
        matrices.push();
        matrices.scale(frameScale, frameScale, frameScale);
        drawModel(matrices, frameModel, vertexConsumers, new Identifier("fpv20", "textures/item/drone_carbon.png"), light, overlay);
        matrices.pop();

        // 2. Draw Battery on top plate
        float batScaleX = 1.0f;
        float batScaleY = 1.0f;
        float batScaleZ = 1.0f;
        switch (batteryIndex) {
            case 0: // 3S
                batScaleX = 0.75f;
                batScaleY = 0.6f;
                batScaleZ = 0.75f;
                break;
            case 1: // 4S 850
                batScaleX = 0.82f;
                batScaleY = 0.7f;
                batScaleZ = 0.82f;
                break;
            case 2: // 4S 1300
                batScaleX = 0.9f;
                batScaleY = 0.8f;
                batScaleZ = 0.9f;
                break;
            case 3: // 6S 1300
                batScaleX = 1.0f;
                batScaleY = 1.0f;
                batScaleZ = 1.0f;
                break;
            case 4: // 6S 1800
                batScaleX = 1.1f;
                batScaleY = 1.15f;
                batScaleZ = 1.1f;
                break;
        }

        matrices.push();
        matrices.translate(0, 3.5f * frameScale, 0);
        matrices.scale(batScaleX * frameScale, batScaleY * frameScale, batScaleZ * frameScale);
        drawModel(matrices, batteryModel, vertexConsumers, new Identifier("fpv20", "textures/item/drone_battery.png"), light, overlay);
        matrices.pop();

        // 3. Draw Camera / Payload
        if (cameraIndex > 0) {
            matrices.push();
            matrices.translate(0, 3.5f * frameScale, 3.0f * frameScale);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25f));
            
            float cx = (cameraIndex == 2) ? 3.0f : 2.4f;
            float cy = (cameraIndex == 2) ? 2.4f : 1.8f;
            float cz = (cameraIndex == 2) ? 2.2f : 1.3f;
            
            VertexConsumer camConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(new Identifier("fpv20", "textures/item/drone_metal.png")));
            Matrix4f camMatrix = matrices.peek().getPositionMatrix();
            // Draw 6 faces of GoPro
            drawFace(camMatrix, camConsumer, -cx, cy, -cz, cx, cy, cz, 0, 1, 0, light, overlay);
            drawFace(camMatrix, camConsumer, -cx, -cy, -cz, cx, -cy, cz, 0, -1, 0, light, overlay);
            drawFace(camMatrix, camConsumer, -cx, -cy, -cz, cx, cy, -cz, 0, 0, -1, light, overlay);
            drawFace(camMatrix, camConsumer, -cx, -cy, cz, cx, cy, cz, 0, 0, 1, light, overlay);
            drawFace(camMatrix, camConsumer, -cx, -cy, -cz, -cx, cy, cz, -1, 0, 0, light, overlay);
            drawFace(camMatrix, camConsumer, cx, -cy, -cz, cx, cy, cz, 1, 0, 0, light, overlay);
            
            matrices.pop();
        }

        // 4. Draw Motors & Propellers at the 4 motor mounts
        float[][] motorCoords = {
                {motorDistance, 0.5f * frameScale, motorDistance},
                {-motorDistance, 0.5f * frameScale, motorDistance},
                {motorDistance, 0.5f * frameScale, -motorDistance},
                {-motorDistance, 0.5f * frameScale, -motorDistance}
        };

        float motorScale = 1.0f;
        float motorHeight = 3.5f;
        switch (motorIndex) {
            case 0: // 1204
                motorScale = 0.55f;
                motorHeight = 2.0f;
                break;
            case 1: // 1404
                motorScale = 0.7f;
                motorHeight = 2.6f;
                break;
            case 2: // 2207 (6S)
            case 3: // 2207 (4S)
                motorScale = 1.0f;
                motorHeight = 3.5f;
                break;
            case 4: // 2807
                motorScale = 1.25f;
                motorHeight = 4.4f;
                break;
        }

        float propScale = propDiameter / 5.0f;

        for (int i = 0; i < 4; i++) {
            float[] coord = motorCoords[i];
            matrices.push();
            matrices.translate(coord[0], coord[1], coord[2]);
            
            // Draw motor
            matrices.push();
            matrices.scale(motorScale * frameScale, motorScale * frameScale, motorScale * frameScale);
            drawModel(matrices, motorModel, vertexConsumers, new Identifier("fpv20", "textures/item/drone_metal.png"), light, overlay);
            matrices.pop();

            // Draw propeller on top of motor
            matrices.push();
            matrices.translate(0, motorHeight * motorScale * frameScale, 0);
            matrices.scale(propScale * frameScale, frameScale, propScale * frameScale);
            
            float direction = (i == 0 || i == 3) ? -1f : 1f;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(propRotationAngle * direction));

            drawModel(matrices, propellerModel, vertexConsumers, new Identifier("fpv20", "textures/item/drone_propeller.png"), light, overlay);

            matrices.pop(); // prop
            matrices.pop(); // motor
        }

        matrices.pop(); // overall
    }

    private static void drawModel(MatrixStack matrices, DroneModel model, VertexConsumerProvider vertexConsumers, Identifier defaultTexture, int light, int overlay) {
        if (model == null || model.cubes.isEmpty()) return;

        for (ModelCube cube : model.cubes) {
            matrices.push();

            // Handle custom cube rotation
            if (cube.rotation != null) {
                // Translate to pivot
                matrices.translate(cube.origin[0], cube.origin[1], cube.origin[2]);
                if (cube.rotation[2] != 0) {
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cube.rotation[2]));
                }
                if (cube.rotation[1] != 0) {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cube.rotation[1]));
                }
                if (cube.rotation[0] != 0) {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cube.rotation[0]));
                }
                // Translate back from pivot
                matrices.translate(-cube.origin[0], -cube.origin[1], -cube.origin[2]);
            }

            float minX = cube.from[0];
            float minY = cube.from[1];
            float minZ = cube.from[2];
            float maxX = cube.to[0];
            float maxY = cube.to[1];
            float maxZ = cube.to[2];

            Matrix4f boxMatrix = matrices.peek().getPositionMatrix();

            VertexConsumer upConsumer = getConsumerForFace(model, vertexConsumers, defaultTexture, cube.texUp);
            VertexConsumer downConsumer = getConsumerForFace(model, vertexConsumers, defaultTexture, cube.texDown);
            VertexConsumer northConsumer = getConsumerForFace(model, vertexConsumers, defaultTexture, cube.texNorth);
            VertexConsumer southConsumer = getConsumerForFace(model, vertexConsumers, defaultTexture, cube.texSouth);
            VertexConsumer westConsumer = getConsumerForFace(model, vertexConsumers, defaultTexture, cube.texWest);
            VertexConsumer eastConsumer = getConsumerForFace(model, vertexConsumers, defaultTexture, cube.texEast);

            // 1. Up (+Y)
            drawFace(boxMatrix, upConsumer, minX, maxY, minZ, maxX, maxY, maxZ, 0, 1, 0, cube.uvUp, light, overlay);
            // 2. Down (-Y)
            drawFace(boxMatrix, downConsumer, minX, minY, minZ, maxX, minY, maxZ, 0, -1, 0, cube.uvDown, light, overlay);
            // 3. North (-Z)
            drawFace(boxMatrix, northConsumer, minX, minY, minZ, maxX, maxY, minZ, 0, 0, -1, cube.uvNorth, light, overlay);
            // 4. South (+Z)
            drawFace(boxMatrix, southConsumer, minX, minY, maxZ, maxX, maxY, maxZ, 0, 0, 1, cube.uvSouth, light, overlay);
            // 5. West (-X)
            drawFace(boxMatrix, westConsumer, minX, minY, minZ, minX, maxY, maxZ, -1, 0, 0, cube.uvWest, light, overlay);
            // 6. East (+X)
            drawFace(boxMatrix, eastConsumer, maxX, minY, minZ, maxX, maxY, maxZ, 1, 0, 0, cube.uvEast, light, overlay);

            matrices.pop();
        }
    }

    private static VertexConsumer getConsumerForFace(DroneModel model, VertexConsumerProvider vertexConsumers, Identifier defaultTexture, int textureKey) {
        Identifier tex = model.textureMap.get(textureKey);
        if (tex == null) {
            tex = defaultTexture;
        }
        return vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
    }

    private static void drawFace(Matrix4f matrix, VertexConsumer consumer, float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ, float nx, float ny, float nz, int light, int overlay) {
        drawFace(matrix, consumer, minX, minY, minZ, maxX, maxY, maxZ, nx, ny, nz, new float[]{0, 0, 16, 16}, light, overlay);
    }

    private static void drawFace(Matrix4f matrix, VertexConsumer consumer, float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ, float nx, float ny, float nz, float[] uv, int light, int overlay) {
        float u1 = uv[0] / 16.0f;
        float v1 = uv[1] / 16.0f;
        float u2 = uv[2] / 16.0f;
        float v2 = uv[3] / 16.0f;

        if (ny != 0) {
            vertex(matrix, consumer, minX, minY, minZ, u1, v1, nx, ny, nz, light, overlay);
            vertex(matrix, consumer, minX, minY, maxZ, u1, v2, nx, ny, nz, light, overlay);
            vertex(matrix, consumer, maxX, minY, maxZ, u2, v2, nx, ny, nz, light, overlay);
            vertex(matrix, consumer, maxX, minY, minZ, u2, v1, nx, ny, nz, light, overlay);
        } else if (nx != 0) {
            vertex(matrix, consumer, minX, minY, minZ, u1, v2, nx, ny, nz, light, overlay);
            vertex(matrix, consumer, minX, maxY, minZ, u1, v1, nx, ny, nz, light, overlay);
            vertex(matrix, consumer, minX, maxY, maxZ, u2, v1, nx, ny, nz, light, overlay);
            vertex(matrix, consumer, minX, minY, maxZ, u2, v2, nx, ny, nz, light, overlay);
        } else {
            vertex(matrix, consumer, minX, minY, minZ, u1, v2, nx, ny, nz, light, overlay);
            vertex(matrix, consumer, maxX, minY, minZ, u2, v2, nx, ny, nz, light, overlay);
            vertex(matrix, consumer, maxX, maxY, minZ, u2, v1, nx, ny, nz, light, overlay);
            vertex(matrix, consumer, minX, maxY, minZ, u1, v1, nx, ny, nz, light, overlay);
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int light, int overlay) {
        Vector4f pos = new Vector4f(x, y, z, 1.0f);
        pos.mul(matrix);
        consumer.vertex(pos.x(), pos.y(), pos.z(), 1.0f, 1.0f, 1.0f, 1.0f, u, v, overlay, light, nx, ny, nz);
    }
}
