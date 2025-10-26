package org.ywzj.vehicle.client.render.model;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.ywzj.vehicle.entity.misc.FakePlayer;

public class FakePlayerModel extends PlayerModel<FakePlayer> {

    public FakePlayerModel(ModelPart modelPart) {
        super(modelPart, false);
    }

}
