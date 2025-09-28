package org.ywzj.vehicle.custom.vehicle;

import org.ywzj.vehicle.entity.vehicle.AbstractVehicle;

public class VehicleIndex<E extends AbstractVehicle, D extends BaseVehicleData> {
    public static final String MAIN_BONE_NAME = "vehicle_body";

    private float width;
    private float length;
    private float height;

//    // 读取结构定义模型，缓存结果
//    protected void initOBBs(List<PartUnit> partUnits, D data) {
//        BedrockModel model = BedrockModelLoader.getModel(data.getStructureModel());
//        BedrockBone bone = model.getBoneMap().get(MAIN_BONE_NAME);
//        // 约定取体积最大的块表达车体的长宽高
//        List<BedrockCubePerFace> cubes = new ArrayList<>(bone.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
//        cubes.sort(
//                (cube1, cube2) -> (int) (cube1.getDepth() * cube1.getWidth() * cube1.getHeight() - cube2.getDepth() * cube2.getWidth() * cube2.getHeight())
//        );
//
//        List<VehicleBedrockCubeOBB> vehicleBodyOBBs = new ArrayList<>();
//        VehicleBedrockCubeOBB mainCubeOBB;
//
//        float width = cubes.get(0).getWidth();
//        float length = cubes.get(0).getDepth();
//        float height = cubes.get(0).getHeight();
//
//        mainCubeOBB = VehicleBedrockCubeOBB.init(vehicle, bone, cubes.remove(0));
//        vehicleBodyOBBs.add(mainCubeOBB);
//
//        for (BedrockCubePerFace cube : cubes) {
//            vehicleBodyOBBs.add(VehicleBedrockCubeOBB.init(vehicle, bone, cube));
//        }
//        for (BedrockBone child : bone.getChildren()) {
//            List<BedrockCubePerFace> childCubes = new ArrayList<>(child.cubes.stream().map(cube -> (BedrockCubePerFace) cube).toList());
//            for (BedrockCubePerFace cube : childCubes) {
//                vehicleBodyOBBs.add(VehicleBedrockCubeOBB.init(vehicle, child, cube));
//            }
//        }
//        // 由部件结构拓展车体长宽
//        for (PartUnit partUnit : partUnits) {
//            for (VehicleBedrockCubeOBB unitOBB : partUnit.getUnitBedrockCubeOBBs()) {
//                Vec3 cubeOffset = unitOBB.offset();
//                width = (float) Math.max((Math.abs(cubeOffset.x) + unitOBB.cube().getWidth() / 2) * 2, width);
//                length = (float) Math.max((Math.abs(cubeOffset.z) + unitOBB.cube().getDepth() / 2) * 2, length);
//                height = (float) Math.max(Math.abs(cubeOffset.y) + unitOBB.cube().getHeight() / 2, height);
//            }
//        }
//
//        this.width = width;
//        this.length = length;
//        this.height = height;
//    }
}
