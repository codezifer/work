-- Pandoc-Lua-Filter: gibt Tabellenspalten proportionale Breiten, damit
-- LaTeX umbrechende p-Spalten statt starrer l-Spalten erzeugt.
-- Ohne Breitenangaben wuerde pandoc `{@{}ll...@{}}` schreiben und lange
-- Zellen liefen ueber den Seitenrand hinaus.
function Table(tbl)
  local ncol = #tbl.colspecs
  if ncol == 0 then return nil end

  local maxlen = {}
  local maxword = {}
  for i = 1, ncol do maxlen[i] = 1; maxword[i] = 1 end

  local function consider(cell)
    local s = ""
    if cell.contents ~= nil then
      s = pandoc.utils.stringify(cell.contents)
    else
      s = pandoc.utils.stringify(cell)
    end
    return #s
  end

  local function consider_rows(rows)
    for _, row in ipairs(rows) do
      for i, cell in ipairs(row.cells) do
        if i <= ncol then
          local n = consider(cell)
          if n > maxlen[i] then maxlen[i] = n end
          local s = ""
          if cell.contents ~= nil then
            s = pandoc.utils.stringify(cell.contents)
          else
            s = pandoc.utils.stringify(cell)
          end
          -- LaTeX bricht in p-Spalten an Leerzeichen und expliziten
          -- Bindestrichen um; nur Segmente ohne solche Stellen sind
          -- unumbrechbar (z. B. Code-Bezeichner mit Unterstrich).
          for seg in s:gmatch("[^%s-]+") do
            if #seg > maxword[i] then maxword[i] = #seg end
          end
        end
      end
    end
  end

  if tbl.head ~= nil and tbl.head.rows ~= nil then
    consider_rows(tbl.head.rows)
  end
  for _, body in ipairs(tbl.bodies) do
    if body.body ~= nil then consider_rows(body.body) end
    if body.head ~= nil then consider_rows(body.head) end
  end
  if tbl.foot ~= nil and tbl.foot.rows ~= nil then
    consider_rows(tbl.foot.rows)
  end

  -- Mindestbreiten: Sockel 12 % plus Platz fuers laengste Wort.
  -- Bedarfe werden zuerst reserviert, der Rest proportional verteilt —
  -- so kann Renormierung nie wieder unter den Bedarf druecken.
  local need = {}
  local need_sum = 0
  for i = 1, ncol do
    need[i] = math.max(0.12, maxword[i] * 0.018)
    need_sum = need_sum + need[i]
  end
  -- Erste Spalte (Stichwort) kompakt halten, aber nie unter Bedarf.
  local cap1 = math.max(0.25, need[1])
  local widths = {}
  local total = 0
  for i = 1, ncol do total = total + maxlen[i] end
  for i = 1, ncol do widths[i] = maxlen[i] / total end
  if ncol > 1 and widths[1] > cap1 then
    local excess = widths[1] - cap1
    widths[1] = cap1
    local rest = 0
    for i = 2, ncol do rest = rest + widths[i] end
    for i = 2, ncol do widths[i] = widths[i] + excess * (widths[i] / rest) end
  end
  local widths_sum = 0
  for i = 1, ncol do widths_sum = widths_sum + widths[i] end
  if need_sum >= 1 then
    for i = 1, ncol do tbl.colspecs[i][2] = need[i] / need_sum end
  else
    local extra = 1 - need_sum
    local flex = widths_sum
    for i = 1, ncol do flex = flex - math.min(widths[i], need[i]) end
    if flex <= 0 then flex = 1 end
    for i = 1, ncol do
      local give = math.max(0, widths[i] - need[i])
      tbl.colspecs[i][2] = need[i] + extra * (give / flex)
    end
  end
  return tbl
end
