package xyz.suiwo.action.service.impl;

import xyz.suiwo.action.service.ModifyService;
import xyz.suiwo.mvcframework.annotation.SWService;

@SWService
public class ModifyServiceImpl implements ModifyService {
    @Override
    public String get() {
        return "This is ModifyServiceImpl";
    }
}
