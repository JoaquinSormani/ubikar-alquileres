package com.ubikar.controlador;

import com.ubikar.dao.InquilinoDAO;
import com.ubikar.modelo.Inquilino;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/inquilinos")
public class InquilinoControlador {

    private final InquilinoDAO inquilinoDAO = new InquilinoDAO();

    @GetMapping
    public List<Inquilino> getInquilinos() {
        return inquilinoDAO.selectAll();
    }

    @PostMapping
    public Inquilino insertInquilino(@RequestBody Inquilino inquilino) {
        Integer id = inquilinoDAO.insert(inquilino);
        inquilino.setId(id);
        return inquilino;
    }

    @DeleteMapping("/{id}")
    public void deleteInquilino(@PathVariable Integer id) {
        inquilinoDAO.delete(id);
    }
}
