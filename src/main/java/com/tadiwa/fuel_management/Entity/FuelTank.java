package com.tadiwa.fuel_management.Entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "fuel_tank")
public class FuelTank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "capacity")
    private Double capacity;

    @OneToMany(mappedBy = "fuelTank", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<FuelTransaction> transactions;

    public FuelTank() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Double getCapacity() { return capacity; }
    public void setCapacity(Double capacity) { this.capacity = capacity; }

    public List<FuelTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<FuelTransaction> transactions) { this.transactions = transactions; }
}
