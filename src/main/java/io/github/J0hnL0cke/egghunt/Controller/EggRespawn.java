package io.github.J0hnL0cke.egghunt.Controller;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import io.github.J0hnL0cke.egghunt.Model.Configuration;
import io.github.J0hnL0cke.egghunt.Model.Data;

public class EggRespawn {
    public static void spawnEggItem(Location loc, Configuration config, Data data){
		ItemStack egg=new ItemStack(Material.DRAGON_EGG);
		egg.setAmount(1);
		Item drop=loc.getWorld().dropItem(loc, egg);
		drop.setGravity(false);
		drop.setGlowing(true);
		drop.setVelocity(new Vector().setX(0).setY(0).setZ(0));
        if (config.getEggInvincible()) {
            drop.setInvulnerable(true);
        }
        data.updateEggLocation(drop);
    }
}
