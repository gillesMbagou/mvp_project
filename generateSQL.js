/**
 * Génère le DDL SQL à partir de votre tableau ENTITIES
 */
function generateDDL(entities) {
    let sql = "-- Script généré automatiquement pour CareSync\n\n";

    entities.forEach((entity) => {
        sql += `CREATE TABLE ${entity.id.toLowerCase()} (\n`;

        const lines = entity.attrs.map(attr => {
            let line = `  ${attr.name.toLowerCase()} ${attr.type.replace(" ENCRYPTED", "")}`;
            if (attr.pk) line += " PRIMARY KEY";
            if (attr.unique) line += " UNIQUE";
            return line;
        });

        // Gestion des Foreign Keys (ajoutées en bas de table)
        entity.attrs.filter(a => a.fk).forEach(fk => {
            lines.push(`  CONSTRAINT fk_${entity.id.toLowerCase()}_${fk.name.replace('_id', '')} FOREIGN KEY (${fk.name.toLowerCase()}) REFERENCES ${fk.fk.toLowerCase()}(id)`);
        });

        sql += lines.join(",\n");
        sql += `\n);\n\n`;
    });

    return sql;
}

// Pour afficher le résultat dans la console :
console.log(generateDDL(ENTITIES));