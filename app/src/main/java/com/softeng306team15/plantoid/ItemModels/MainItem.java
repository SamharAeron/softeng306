package com.softeng306team15.plantoid.ItemModels;

import java.util.List;

public class MainItem extends Item{

    public MainItem(String id, String category, String itemName, float itemPrice, List<String> pics, List<String> tags){
        this.id = id;
        this.category = category;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.images = pics;
        this.tags = tags;
    }

    public MainItem(){

    }
}
