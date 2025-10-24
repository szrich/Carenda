/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package hu.carenda.app.ui;

import hu.carenda.app.model.ServiceJobCard;

/**
 *
 * @author szric
 */
public class ServiceJobCardFormController {
    
    

    private ServiceJobCard editing;

    void setEditing(ServiceJobCard editing) {
        this.editing = editing;

        if (editing != null) {
            // űrlap kitöltése szerkesztéskor
        } else {
            // új rekord – alapértékek
            
        }
    }
    
}
