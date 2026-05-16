{{- define "mirems-platform.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mirems-platform.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "mirems-platform.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mirems-platform.labels" -}}
app.kubernetes.io/name: {{ include "mirems-platform.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: mirems-platform
{{- end -}}

{{- define "mirems-platform.coreApiSecretName" -}}
{{- if .Values.coreApi.secrets.existingSecret -}}
{{- .Values.coreApi.secrets.existingSecret -}}
{{- else -}}
{{- printf "%s-core-api" (include "mirems-platform.fullname" .) -}}
{{- end -}}
{{- end -}}
