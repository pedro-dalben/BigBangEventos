package com.meumod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeuModInitializer implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("meumod");

    @Override
    public void onInitialize() {
        LOG.info("MeuMod inicializado");
    }
}
