(ns net.cgrand.automata)

;; a NFA is a map of states to transitions
;; :init, :ok and nil/false are standard states

{"local *:" {\l "ocal *:"}
 "ocal *:" {\o "cal *:"}
 "cal *:" {\c "al *:"}
 "al *:" {\a "l *:"}
 "l *:" {\l " *:"}
 " *:" {\space " *:"
        \: ""}
 "" {}}

