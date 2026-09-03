package com.integrador.Turismo.Repository;
import com.integrador.Turismo.Model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProveedorRepository extends JpaRepository<Proveedor, String>{
    List<Proveedor> findByTipo(Proveedor.Tipo tipo);
}
